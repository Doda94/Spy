# Server (`server/`)

Cloudflare Worker **`spy-v2`** s bazom **`spy-dictionaries`** (D1): ugrađeni rječnici za aplikacije iz
[`mobile/`](../mobile). Rječnik → kategorija → riječ, a svaki rječnik ima svoj jezik: **Hrvatski**
(iz [`data/words.json`](../data/words.json)) i **English** (iz [`data/words.en.json`](../data/words.en.json),
nacrt za pregled).

Potpuno je odvojen od stare web aplikacije: drugi Worker, druga baza, drugi PIN. Produkcijski
Worker `spy` i baza `spy-words` s grane `main` nisu spomenuti ni u jednoj konfiguraciji ovdje.

Aplikacije **čitaju** bez prijave. **Uređivanje** (dodavanje i brisanje riječi) traži zajednički PIN
u zaglavlju `x-spy-pin`, isto kao u staroj aplikaciji; PIN je secret `ADMIN_PIN`.

## Mapa

| Datoteka | Što je |
| --- | --- |
| [`src/index.ts`](src/index.ts) | rute i odgovori |
| [`src/dictionaries.ts`](src/dictionaries.ts) | upiti nad D1 (popis, sadržaj, dodavanje, brisanje) |
| [`src/auth.ts`](src/auth.ts) | provjera PIN-a |
| [`src/http.ts`](src/http.ts) | JSON odgovori, ETag / `If-None-Match` |
| [`src/words.ts`](src/words.ts) | pravila za riječi i kategorije (iz stare aplikacije) |
| [`migrations/0001_dictionaries.sql`](migrations/0001_dictionaries.sql) | shema baze |
| [`seed.sql`](seed.sql) | **generirano** iz `data/*.json` (`npm run seed:generate`); ne uređivati ručno |
| [`test/`](test) | vitest + workerd (`@cloudflare/vitest-plugin`) |

## Razvoj i testovi

Treba Node 22+.

```bash
cd server
npm ci
npm test                  # testovi u workerd-u, s pravom D1 shemom i seedom
npm run typecheck
npm run build:check       # `wrangler deploy --dry-run`: samo gradi, ništa ne objavljuje
npm run seed:check        # seed.sql mora odgovarati data/*.json
```

Lokalni Worker s lokalnom kopijom baze (ništa se ne šalje na Cloudflare):

```bash
printf 'ADMIN_PIN=1234\n' > .dev.vars
npm run db:setup:local    # migracije + seed u .wrangler/state
npm run dev               # http://localhost:8787
```

CI ([`.github/workflows/server.yml`](../.github/workflows/server.yml)) pokreće tipove, testove,
provjeru seeda i `build:check` na PR-ovima prema `v2` i složenim `feat/v2-*` PR-ovima.

## Prvo postavljanje (radi vlasnik)

Ništa od ovoga nije pokrenuto iz repozitorija. Treba Cloudflare račun s prijavljenim `wrangler`-om
(`npx wrangler login`).

```bash
cd server
npm ci
npx wrangler d1 create spy-dictionaries   # ispiše database_id
```

Upiši `database_id` u [`wrangler.toml`](wrangler.toml) (zamijeni nule), pa:

```bash
npm run db:setup:remote                   # migracije + seed na spy-dictionaries
npx wrangler deploy                       # objavi Worker spy-v2
npx wrangler secret put ADMIN_PIN         # upiši PIN za uređivanje
```

Provjera (adresa se ispiše pri deployu):

```bash
curl https://spy-v2.<poddomena>.workers.dev/v1/dictionaries
```

Dok `ADMIN_PIN` nije postavljen, izmjene vraćaju `503 pin_not_configured`, a čitanje radi.
`npm run db:setup:remote` je sigurno ponoviti: dodaje što fali i podiže verziju rječnika. Obrisane
riječi koje su u `data/*.json` pritom se vrate (JSON je izvor istine za početni sadržaj).

## Ugovor API-ja (v1)

Sve rute su pod `/v1`. Tijela su JSON (`content-type: application/json`), tekst je UTF-8.
Greške su uvijek `{ "error": "<kod>" }` (kodovi niže). Bazni URL je adresa Workera `spy-v2`.

### Čitanje (bez PIN-a)

#### `GET /v1/dictionaries`

Popis rječnika s verzijom. Aplikacija ovo zove pri pokretanju i usporedi `version` s onim što ima
u predmemoriji.

```json
{
  "version": "hr.1792448942,en.1792448942",
  "dictionaries": [
    { "id": "hr", "name": "Hrvatski", "language": "hr", "revision": 1792448942, "categoryCount": 3, "wordCount": 50 },
    { "id": "en", "name": "English", "language": "en", "revision": 1792448942, "categoryCount": 3, "wordCount": 50 }
  ]
}
```

- `id`: stabilan ključ rječnika (mala slova i znamenke, do 32 znaka); aplikacija ga pamti.
- `language`: jezik riječi (ISO 639-1).
- `revision`: broj koji raste sa svakom izmjenom tog rječnika. **Neproziran je**: uspoređuje se
  samo na jednakost, ne smije se tumačiti kao vrijeme ni brojač.
- Redoslijed je redoslijed prikaza (Hrvatski, pa English).

#### `GET /v1/dictionaries/:id`

Cijeli sadržaj jednog rječnika. Kategorije su u redoslijedu prikaza, riječi su sortirane po jeziku
rječnika.

```json
{
  "id": "hr", "name": "Hrvatski", "language": "hr", "revision": 1792448942,
  "categories": [
    { "id": 1, "name": "Mjesta", "words": [ { "id": 1, "text": "Banka" }, { "id": 7, "text": "Bolnica" } ] }
  ]
}
```

`id` kategorija i riječi služi uređivanju (brisanje); igra koristi samo nazive i tekst. Nepoznat ili
neispravan `:id` vraća `404 not_found`.

#### Predmemorija i offline

Oba odgovora imaju slabu oznaku `ETag` i `Cache-Control: no-cache`. Preporučeni tijek za aplikaciju:

1. `GET /v1/dictionaries` s `If-None-Match: <spremljeni ETag>`.
   - `304` bez tijela: ništa se nije promijenilo, koristi se predmemorija.
   - `200`: za svaki rječnik čiji se `revision` razlikuje od spremljenog (ili ga nema) dohvati
     `GET /v1/dictionaries/:id` i spremi ga zajedno s ETagom iz odgovora.
2. Rječnik koji je nestao s popisa treba maknuti iz predmemorije.
3. Bez mreže ili uz grešku servera igra se iz zadnje spremljene predmemorije.

`ETag` popisa je `W/"<version>"`, a pojedinog rječnika `W/"<id>.<revision>"`. `HEAD` radi na oba
čitanja. Odgovori ne sadrže ništa tajno.

### Uređivanje (zaglavlje `x-spy-pin`)

Svaka izmjena šalje `x-spy-pin: <PIN>`. Bez PIN-a ili s krivim: `401 unauthorized`. Ako Worker nema
postavljen `ADMIN_PIN`: `503 pin_not_configured`. PIN se provjeri prije bilo čega drugog. Odgovori
na izmjene imaju `Cache-Control: no-store`.

#### `POST /v1/auth`

Provjera PIN-a pri otključavanju uređivanja u aplikaciji. `200 { "ok": true }`.

#### `POST /v1/dictionaries/:id/words`

Tijelo: `{ "text": "Most", "category": "Mjesta" }`.

- `201 { "word": { "id": 51, "text": "Most", "category": "Mjesta" } }`
- Kategorija koja već postoji (bez obzira na velika/mala slova) zadržava svoj zapisani naziv, pa
  `"mjesta"` ne stvara drugu kategoriju uz `"Mjesta"`. Nova kategorija ide na kraj popisa.
- `409 { "error": "duplicate", "word": { "id", "text", "category" } }` ako riječ već postoji u tom
  rječniku (u bilo kojoj kategoriji, bez obzira na velika/mala slova). Svaki rječnik ima svoj skup riječi.

#### `DELETE /v1/dictionaries/:id/words/:wordId`

`204` bez tijela. Kategorija bez riječi nestaje (kategorije se izvode iz riječi).
`404 not_found` ako riječi nema u tom rječniku, `400 invalid_id` ako `:wordId` nije cijeli broj veći od 0.

Svaka uspješna izmjena podiže `revision` tog rječnika, pa se mijenjaju i ETag rječnika i ETag popisa.

### Greške

| Status | `error` | Kad |
| --- | --- | --- |
| 400 | `invalid_body` | tijelo nije JSON ili `text` / `category` nisu stringovi |
| 400 | `empty` | riječ je prazna nakon čišćenja razmaka |
| 400 | `too-long` | riječ je dulja od 60 znakova |
| 400 | `category-empty` | kategorija je prazna |
| 400 | `category-too-long` | kategorija je dulja od 40 znakova |
| 400 | `invalid_id` | `:wordId` nije valjan |
| 401 | `unauthorized` | PIN nedostaje ili je kriv |
| 404 | `not_found` | nepoznata ruta, rječnik ili riječ |
| 405 | `method_not_allowed` | poznata ruta, krivi način; zaglavlje `Allow` kaže koji su dopušteni |
| 409 | `duplicate` | riječ već postoji u rječniku |
| 500 | `server` | neočekivana greška |
| 503 | `pin_not_configured` | `ADMIN_PIN` nije postavljen |

Kodovi su isti kao u staroj web aplikaciji, uz `invalid_body`, `invalid_id` i `method_not_allowed`.

### Pravila za riječi

- Razmaci na rubovima se miču, a višestruki razmaci u sredini postaju jedan.
- Riječ do 60 znakova, kategorija do 40.
- Duplikati se prepoznaju po malim slovima prema jeziku rječnika (`Škola` = `ŠKOLA`).

## Model podataka

`dictionaries (id, name, language, position, revision)` → `categories (id, dictionary_id, name,
normalized, position)` → `words (id, dictionary_id, category_id, text, normalized)`. Riječ je
jedinstvena unutar rječnika, naziv kategorije također. Shema osigurava da kategorija riječi pripada
istom rječniku, a brisanje rječnika briše njegove kategorije i riječi.

## Ograničenja (namjerno, za sada)

- **Nema izrade, preimenovanja ni brisanja rječnika preko API-ja.** Rječnici se dodaju u
  `scripts/generate-seed.mjs` i `data/`, a zatim se seed ponovno primijeni.
- **PIN nema ograničenje broja pokušaja.** Kratki numerički PIN može se pogađati. Ako to smeta,
  dodaj Cloudflare rate limiting pravilo za `POST /v1/*`.
- **Nema CORS zaglavlja**: pozivaju je samo nativne aplikacije.
- **Engleski rječnik je nacrt** (isti oblik kao hrvatski: 3 kategorije, 50 riječi) i čeka pregled.
- Sobe za igru (Durable Objects i WebSocket) dolaze u milestonu 6, a protokol u `server/PROTOCOL.md`.
