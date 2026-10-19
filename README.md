# Špijun

> **v2 (ova grana):** nativne iOS i Android aplikacije u Kotlin Multiplatform + Compose
> Multiplatform nalaze se u [`mobile/`](mobile). Dizajn je u [`docs/design`](docs/design).
> Web aplikacija opisana ispod ostaje u repozitoriju dok je ne zamijeni v2.

## Mobilne aplikacije (`mobile/`)

| Modul | Što je |
| --- | --- |
| `shared` | Pravila igre bez UI-ja (uloge, runde, glasanje, neriješeno, pobjeda) + jedinični testovi |
| `composeApp` | Zajednički Compose UI, tema (tokeni iz dizajna), fontovi, tekstovi HR/EN |
| `androidApp` | Android ljuska (`MainActivity`) |
| `iosApp` | Xcode projekt (SwiftUI ljuska koja prikazuje Compose) |

Što radi: igra na jednom telefonu od početka do kraja (početna, postavke, otkrivanje uloga na
okruglom stolu, rasprava s timerom, glasanje u aplikaciji ili naglas, rezultat runde, kraj igre)
i kartica **Rječnici**: vlastiti rječnici (izrada, kategorije, riječi, brisanje s poništavanjem),
izvoz (datoteka, poveznica, QR) i uvoz (datoteka ili poveznica `spijun://dictionary?d=…`) s
pregledom i spajanjem. Vlastiti rječnici žive samo na telefonu (`dictionaries.json`); pravila su u
`shared/.../dictionaries` i imaju testove.
Najviše špijuna je ⌊(igrači − 1) / 2⌋ (odluka vlasnika, `Rules.maxSpies`).

```bash
cd mobile
./gradlew :shared:testAndroidHostTest      # testovi pravila igre
./gradlew :androidApp:installDebug         # Android (emulator ili uređaj)
open iosApp/iosApp.xcodeproj               # iOS: Run na simulatoru (Gradle se pokreće iz Xcodea)
```

Treba JDK 21+ i Android SDK (`sdk.dir` u `mobile/local.properties` ili `ANDROID_HOME`).
U debug buildu se `FLAG_SECURE` ne postavlja da bi snimke zaslona radile; u release buildu
je uključen tijekom otkrivanja uloge.

CI (`.github/workflows/mobile.yml`) pokreće testove pravila na JVM-u i na iOS simulatoru, gradi
debug APK i iOS aplikaciju za simulator, na PR-ovima prema `v2` i složenim `feat/v2-*` PR-ovima.

---

Društvena igra "Špijun" za jedan telefon — svi igrači redom pogledaju svoju ulogu na
istom uređaju, a zatim slijedi rasprava uživo. Sučelje je u cijelosti na hrvatskom.

## Pravila

- Igra ima X igrača, od kojih je Y tajno špijun (Y < X).
- Prije početka bira se jedna ili više kategorija; riječ se izvlači iz njih.
- Svi osim špijuna vide tajnu riječ. Špijun vidi "Ti si špijun!" i kategoriju —
  to mu je jedini trag.
- Kad svi pogledaju svoju ulogu, aplikacija otvara zaslon rasprave. Rasprava i
  glasanje odvijaju se uživo, izvan aplikacije.

## Kako je složeno

Next.js (App Router) + TypeScript, obični CSS. Stranica se gradi kao statički izvoz
(`output: "export"`), a popis riječi živi u Cloudflare D1 bazi iza tri Cloudflare
Pages funkcije. Nema računa ni prijave — uređivanje čuva jedan zajednički PIN.

| Ruta                   | Tko smije            | Što radi                     |
| ---------------------- | -------------------- | ---------------------------- |
| `GET /api/words`       | svi                  | vraća cijeli popis riječi    |
| `POST /api/words`      | samo s ispravnim PIN-om | dodaje riječ              |
| `DELETE /api/words/:id`| samo s ispravnim PIN-om | briše riječ               |
| `POST /api/auth`       | samo s ispravnim PIN-om | provjera PIN-a pri otključavanju |

PIN se šalje u zaglavlju `x-spy-pin`, a provjerava ga [`functions/api/_middleware.ts`](functions/api/_middleware.ts)
za svaki zahtjev koji nije `GET`.

U `localStorage` ostaje samo ono što je vezano uz taj uređaj:

| Ključ             | Sadržaj                                                        |
| ----------------- | -------------------------------------------------------------- |
| `spy.gameState`   | faza, postavke i tekuća partija (preživljava osvježavanje)      |
| `spy.wordsCache`  | zadnji dohvaćeni popis — da se može igrati i bez mreže          |
| `spy.pin`         | PIN, da ga ne treba upisivati svaki put                         |

Struktura koda:

- [`app/page.tsx`](app/page.tsx) — orkestracija faza igre i poziva prema API-ju
- [`components/`](components) — zasloni (početna, postavke, otkrivanje, rasprava)
- [`functions/api/`](functions/api) — Cloudflare Pages funkcije
- [`lib/words.ts`](lib/words.ts) — pravila za riječi, zajednička klijentu i serveru
- [`lib/api.ts`](lib/api.ts) — klijent prema API-ju
- [`lib/game.ts`](lib/game.ts) — nasumična riječ i podjela uloga
- [`lib/storage.ts`](lib/storage.ts) — čitanje/pisanje i provjera spremljenog stanja

## Riječi

Svaka riječ pripada jednoj kategoriji. Kategorije nisu zaseban popis nego se
izvode iz riječi — nova kategorija nastaje čim joj dodaš prvu riječ, a nestane
kad obrišeš zadnju. Pri dodavanju polje "Kategorija" nudi postojeće kategorije,
ali možeš upisati i novu.

Riječ je jedinstvena u cijeloj bazi, neovisno o kategoriji i o velikim slovima:
"Mostar" i "mostar" su ista riječ. Isto vrijedi za nazive kategorija, pa
upisivanje "mjesta" ne stvara drugu kategoriju uz "Mjesta".

[`data/words.json`](data/words.json) je početni popis kojim se baza puni prvi
put, grupiran po kategorijama:

```json
{
  "Mjesta": ["Plaža", "Bolnica"],
  "Zanimanja": ["Liječnik"]
}
```

Ako u `words.json` dodaš nove riječi i želiš ih ubaciti u bazu:

```bash
npm run seed:generate
npx wrangler d1 execute spy-words --remote --file=./seed.sql
```

`seed.sql` koristi `INSERT OR IGNORE`, pa ponovno pokretanje ne stvara duplikate
niti vraća riječi koje si u međuvremenu obrisao.

Za čistu bazu od nule (briše sve riječi pa ponovno sije `words.json`):

```bash
npm run db:reset:remote
```

## Prvo postavljanje

```bash
npm install
npx wrangler d1 create spy-words
```

Zadnja naredba ispiše `database_id` — upiši ga u [`wrangler.toml`](wrangler.toml).
Zatim napuni bazu i postavi PIN:

```bash
npm run db:remote
npx wrangler pages secret put ADMIN_PIN
```

## Lokalni rad

```bash
printf 'ADMIN_PIN=1234\n' > .dev.vars
npm run db:local
npm run preview
```

`npm run preview` gradi statičke datoteke i Worker pa pokreće `wrangler dev` na
http://localhost:8788, s lokalnom kopijom baze. `npm run dev` pokreće samo Next.js
(bez funkcija), pa aplikacija u tom načinu radi iz spremljenog popisa riječi.

## Objava na Cloudflare Pages

U Cloudflare nadzornoj ploči: Workers & Pages → Create → Pages → poveži repozitorij.

| Postavka                | Vrijednost      |
| ----------------------- | --------------- |
| Build command           | `npm run build` |
| Build output directory  | `out`           |
| Framework preset        | Next.js (Static HTML Export) |

Dodaj varijablu okoline `NODE_VERSION` = `20` (ili novije) jer Next 16 traži Node 20+.
U postavkama projekta poveži D1 bazu `spy-words` na binding `DB` i postavi secret
`ADMIN_PIN`. Nakon toga svaki `git push` na `main` objavljuje novu verziju.
