# Špijun

> **v2 (ova grana):** nativne iOS i Android aplikacije u Kotlin Multiplatform + Compose
> Multiplatform nalaze se u [`mobile/`](mobile), pozadina u [`server/`](server). Dizajn je u
> [`docs/design`](docs/design). Stara web aplikacija uklonjena je s ove grane (vidi na dnu).

Špijun je društvena igra: svi igrači redom vide svoju ulogu (špijun ili tajnu riječ), zatim slijedi
rasprava uživo i glasanje za špijuna. Sučelje je na hrvatskom i engleskom.

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

## Server (`server/`)

Cloudflare Worker s ugrađenim rječnicima (rječnik → kategorija → riječ) dolazi u zasebnom PR-u.
Zasad `server/` sadrži samo ono što je preostalo od stare web aplikacije, a treba novom serveru:

| Datoteka | Što je |
| --- | --- |
| [`src/words.ts`](server/src/words.ts) | pravila za riječi i kategorije (čišćenje unosa, ključ za duplikate, ograničenja duljine) |
| [`scripts/generate-seed.mjs`](server/scripts/generate-seed.mjs) | iz [`data/words.json`](data/words.json) generira `server/seed.sql` (`npm run seed:generate` iz mape `server/`) |
| `schema.sql`, `seed.sql` | SQL stare ravne baze riječi; zamjenjuje ih shema rječnika u sljedećem PR-u |

Server za `v2` koristi **novi** Worker i **novu** D1 bazu, nikad produkcijske (`spy`, `spy-words`) s
grane `main`.

## Stara web aplikacija

Prva verzija (Next.js + Cloudflare Pages + D1, jedan telefon, uređivanje riječi preko zajedničkog
PIN-a) živi dalje samo na grani [`main`](https://github.com/Doda94/Spy/tree/main), koju Cloudflare
objavljuje u produkciju. Na `v2` je uklonjena; igra i rječnici sada su u aplikacijama iz `mobile/`.
