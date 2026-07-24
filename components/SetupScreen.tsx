"use client";

import { MAX_PLAYERS, MIN_PLAYERS, type Settings } from "@/lib/types";
import type { WordsStatus } from "./HomeScreen";

type Props = {
  settings: Settings;
  onChange: (settings: Settings) => void;
  wordCount: number;
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
  wordCount,
  status,
  onStart,
  onBack,
}: Props) {
  const maxSpies = settings.playerCount - 1;
  const loading = status === "loading";
  const noWords = !loading && wordCount === 0;

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
        <span className="label">Riječi</span>
        <p className="hint" style={{ textAlign: "center" }}>
          {loading
            ? "Učitavanje popisa…"
            : `Na popisu ${wordCount === 1 ? "je" : "ih je"} ${wordCount}. Jedna se bira nasumično.`}
        </p>
      </div>

      {noWords && (
        <p className="notice">
          Popis riječi je prazan. Dodaj riječi na početnom zaslonu prije igre.
        </p>
      )}

      <div className="spacer" />

      <div className="stack">
        <button
          className="btn btn--primary"
          onClick={onStart}
          disabled={loading || noWords}
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
