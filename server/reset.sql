-- Briše staru tablicu riječi (vidi schema.sql) da se baza može izgraditi od nule.
-- Preseljeno iz web aplikacije na `main`; zamjenjuje se u sljedećem PR-u.

DROP TABLE IF EXISTS words;
