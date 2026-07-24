# Špijun

Društvena igra "Špijun" za jedan telefon — svi igrači redom pogledaju svoju ulogu na
istom uređaju, a zatim slijedi rasprava uživo. Sučelje je u cijelosti na hrvatskom.

## Pravila

- Igra ima X igrača, od kojih je Y tajno špijun (Y < X).
- Svi osim špijuna vide istu tajnu riječ. Špijuni vide samo "Ti si špijun!".
- Kad svi pogledaju svoju ulogu, aplikacija otvara zaslon rasprave. Rasprava i
  glasanje odvijaju se uživo, izvan aplikacije.

## Pokretanje

```bash
npm install
npm run dev
```

Aplikacija je dostupna na http://localhost:3000.

## Riječi

- Ugrađene riječi: [`data/words.json`](data/words.json) — obično polje stringova.
  Popis se slobodno proširuje.
- Vlastite riječi: dodaju se i brišu na početnom zaslonu ("Upravljaj riječima") i
  spremaju se u `localStorage`, odvojeno od ugrađenog popisa.
- Na zaslonu postavki bira se izvor riječi za tu partiju: ugrađene, vlastite ili sve.

## Tehnički detalji

Next.js (App Router) + TypeScript, obični CSS, bez backenda. Sve se odvija na
klijentu; stanje igre i vlastite riječi žive u `localStorage`:

| Ključ              | Sadržaj                                                     |
| ------------------ | ----------------------------------------------------------- |
| `spy.gameState`    | faza, postavke i tekuća igra (preživljava osvježavanje)      |
| `spy.customWords`  | popis vlastitih riječi                                      |

Struktura koda:

- [`app/page.tsx`](app/page.tsx) — orkestracija faza igre
- [`components/`](components) — zasloni (početna, postavke, otkrivanje, rasprava)
- [`lib/game.ts`](lib/game.ts) — bazen riječi, nasumična riječ i podjela uloga
- [`lib/storage.ts`](lib/storage.ts) — čitanje/pisanje i provjera spremljenog stanja
