"use client";

import { MAX_PLAYERS, MIN_PLAYERS, type Settings, type WordSource } from "@/lib/types";

type Props = {
  settings: Settings;
  onChange: (settings: Settings) => void;
  /** Broj riječi po izvoru, nakon uklanjanja duplikata. */
  poolSizes: Record<WordSource, number>;
  onStart: () => void;
  onBack: () => void;
};

const SOURCE_LABELS: { value: WordSource; label: string }[] = [
  { value: "builtin", label: "Ugrađene riječi" },
  { value: "custom", label: "Moje riječi" },
  { value: "all", label: "Sve riječi" },
];

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
  poolSizes,
  onStart,
  onBack,
}: Props) {
  const maxSpies = settings.playerCount - 1;
  const poolSize = poolSizes[settings.wordSource];
  const poolEmpty = poolSize === 0;

  function setPlayerCount(next: number) {
    const playerCount = Math.min(MAX_PLAYERS, Math.max(MIN_PLAYERS, next));
    onChange({
      ...settings,
      playerCount,
      spyCount: Math.min(settings.spyCount, playerCount - 1),
    });
  }

  function setSpyCount(next: number) {
    onChange({
      ...settings,
      spyCount: Math.min(maxSpies, Math.max(1, next)),
    });
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
        <span className="label">Izvor riječi</span>
        <div className="segmented" role="radiogroup" aria-label="Izvor riječi">
          {SOURCE_LABELS.map(({ value, label }) => {
            const active = settings.wordSource === value;
            return (
              <label
                key={value}
                className={`segmented__option${active ? " segmented__option--active" : ""}`}
              >
                <input
                  type="radio"
                  name="wordSource"
                  value={value}
                  checked={active}
                  onChange={() => onChange({ ...settings, wordSource: value })}
                />
                <span className="segmented__dot" aria-hidden="true" />
                <span>{label}</span>
                <span className="segmented__count">{poolSizes[value]}</span>
              </label>
            );
          })}
        </div>
      </div>

      {poolEmpty && (
        <p className="notice">
          {settings.wordSource === "custom"
            ? "Nemaš još nijednu svoju riječ. Dodaj riječi na početnom zaslonu ili odaberi drugi izvor."
            : "Odabrani izvor nema nijednu riječ. Odaberi drugi izvor."}
        </p>
      )}

      <div className="spacer" />

      <div className="stack">
        <button className="btn btn--primary" onClick={onStart} disabled={poolEmpty}>
          Započni igru
        </button>
        <button className="btn btn--ghost" onClick={onBack}>
          Natrag
        </button>
      </div>
    </main>
  );
}
