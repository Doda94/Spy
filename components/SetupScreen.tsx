"use client";

import { MAX_PLAYERS, MIN_PLAYERS, type Settings } from "@/lib/types";
import { wordKey } from "@/lib/words";
import type { WordsStatus } from "./HomeScreen";

type Props = {
  settings: Settings;
  onChange: (settings: Settings) => void;
  /** Sve kategorije koje postoje u bazi, s brojem riječi. */
  categories: { name: string; count: number }[];
  /** Broj riječi u odabranim kategorijama. */
  poolSize: number;
  status: WordsStatus;
  onStart: () => void;
  onBack: () => void;
};

function Stepper({
  value,
  min,
  max,
  onChange,
  label,
}: {
  value: number;
  min: number;
  max: number;
  onChange: (next: number) => void;
  label: string;
}) {
  return (
    <div className="stepper">
      <button
        className="stepper__btn"
        onClick={() => onChange(value - 1)}
        disabled={value <= min}
        aria-label={`${label} — manje`}
      >
        −
      </button>
      <span className="stepper__value" aria-live="polite" aria-label={`${label}: ${value}`}>
        {value}
      </span>
      <button
        className="stepper__btn"
        onClick={() => onChange(value + 1)}
        disabled={value >= max}
        aria-label={`${label} — više`}
      >
        +
      </button>
    </div>
  );
}

export default function SetupScreen({
  settings,
  onChange,
  categories,
  poolSize,
  status,
  onStart,
  onBack,
}: Props) {
  const maxSpies = settings.playerCount - 1;
  const loading = status === "loading";
  // Roditelj uvijek proslijedi razriješen popis; `?? []` je samo za tip.
  const selected = settings.categories ?? [];
  const selectedKeys = new Set(selected.map(wordKey));
  const noneSelected = !loading && selectedKeys.size === 0;
  const emptyPool = !loading && !noneSelected && poolSize === 0;
  const noCategories = !loading && categories.length === 0;

  function setPlayerCount(next: number) {
    const playerCount = Math.min(MAX_PLAYERS, Math.max(MIN_PLAYERS, next));
    onChange({
      ...settings,
      playerCount,
      spyCount: Math.min(settings.spyCount, playerCount - 1),
    });
  }

  function setSpyCount(next: number) {
    onChange({ ...settings, spyCount: Math.min(maxSpies, Math.max(1, next)) });
  }

  function toggleCategory(name: string) {
    const key = wordKey(name);
    const next = selectedKeys.has(key)
      ? selected.filter((c) => wordKey(c) !== key)
      : [...selected, name];
    onChange({ ...settings, categories: next });
  }

  function selectAll() {
    onChange({ ...settings, categories: categories.map((c) => c.name) });
  }

  return (
    <main className="screen">
      <h1 className="screen-title">Postavke igre</h1>

      <div className="field">
        <span className="label">Broj igrača</span>
        <Stepper
          value={settings.playerCount}
          min={MIN_PLAYERS}
          max={MAX_PLAYERS}
          onChange={setPlayerCount}
          label="Broj igrača"
        />
      </div>

      <div className="field">
        <span className="label">Broj špijuna</span>
        <Stepper
          value={settings.spyCount}
          min={1}
          max={maxSpies}
          onChange={setSpyCount}
          label="Broj špijuna"
        />
        <p className="hint" style={{ marginTop: 10, textAlign: "center" }}>
          Kod {settings.playerCount} igrača najviše {maxSpies}{" "}
          {maxSpies === 1 ? "špijun" : "špijuna"}.
        </p>
      </div>

      <div className="field">
        <div className="label-row">
          <span className="label" style={{ marginBottom: 0 }}>
            Kategorije
          </span>
          {categories.length > 1 && selectedKeys.size < categories.length && (
            <button className="link-btn" onClick={selectAll}>
              Odaberi sve
            </button>
          )}
        </div>

        {loading ? (
          <p className="hint" style={{ textAlign: "center" }}>
            Učitavanje…
          </p>
        ) : noCategories ? (
          <p className="hint" style={{ textAlign: "center" }}>
            Nema nijedne riječi u bazi.
          </p>
        ) : (
          <div className="segmented" role="group" aria-label="Kategorije">
            {categories.map(({ name, count }) => {
              const active = selectedKeys.has(wordKey(name));
              return (
                <label
                  key={name}
                  className={`segmented__option${active ? " segmented__option--active" : ""}`}
                >
                  <input
                    type="checkbox"
                    checked={active}
                    onChange={() => toggleCategory(name)}
                  />
                  <span className="segmented__box" aria-hidden="true" />
                  <span>{name}</span>
                  <span className="segmented__count">{count}</span>
                </label>
              );
            })}
          </div>
        )}

        {!loading && !noCategories && !noneSelected && (
          <p className="hint" style={{ marginTop: 12, textAlign: "center" }}>
            {poolSize === 1 ? "1 riječ u izboru" : `${poolSize} riječi u izboru`}.
            Jedna se bira nasumično.
          </p>
        )}
      </div>

      {noneSelected && !noCategories && (
        <p className="notice">Odaberi barem jednu kategoriju.</p>
      )}

      {emptyPool && (
        <p className="notice">
          Odabrane kategorije nemaju nijednu riječ. Odaberi drugu kategoriju.
        </p>
      )}

      {noCategories && (
        <p className="notice">
          Popis riječi je prazan. Dodaj riječi na početnom zaslonu prije igre.
        </p>
      )}

      <div className="spacer" />

      <div className="stack">
        <button
          className="btn btn--primary"
          onClick={onStart}
          disabled={loading || noneSelected || emptyPool || noCategories}
        >
          Započni igru
        </button>
        <button className="btn btn--ghost" onClick={onBack}>
          Natrag
        </button>
      </div>
    </main>
  );
}
