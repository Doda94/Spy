-- Briše tablicu riječi da se baza može ponovno izgraditi od nule.
-- Koristi se preko `npm run db:reset:remote` / `npm run db:reset:local`.

DROP TABLE IF EXISTS words;
