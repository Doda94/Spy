"use client";

import { useRef, useState } from "react";

type Props = {
  customWords: string[];
  onAddWord: (word: string) => "ok" | "empty" | "duplicate";
  onDeleteWord: (word: string) => void;
  onPlay: () => void;
};

export default function HomeScreen({
  customWords,
  onAddWord,
  onDeleteWord,
  onPlay,
}: Props) {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  function handleAdd() {
    const result = onAddWord(draft);
    if (result === "empty") {
      setError("Upiši riječ prije dodavanja.");
      return;
    }
    if (result === "duplicate") {
      setError("Ta riječ već postoji na popisu.");
      return;
    }
    setError(null);
    setDraft("");
    inputRef.current?.focus();
  }

  function toggle() {
    setOpen((prev) => !prev);
    setError(null);
    setPendingDelete(null);
  }

  return (
    <main className="screen">
      <div className="spacer" />

      <div style={{ textAlign: "center" }}>
        <h1 className="title">Špijun</h1>
        <p className="subtitle" style={{ marginTop: 10 }}>
          Jedan telefon, jedna tajna riječ i špijun među vama.
        </p>
      </div>

      <div className="spacer" />

      <button className="btn btn--primary" onClick={onPlay}>
        Igraj
      </button>

      <section className="accordion">
        <button
          className="accordion__toggle"
          onClick={toggle}
          aria-expanded={open}
          aria-controls="upravljanje-rijecima"
        >
          <span>Upravljaj riječima</span>
          <span className="segmented__count">{customWords.length}</span>
          <svg
            className={`accordion__chevron${open ? " accordion__chevron--open" : ""}`}
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </button>

        {open && (
          <div className="accordion__body" id="upravljanje-rijecima">
            <p className="hint">
              Ovdje su samo tvoje riječi. Ugrađene riječi ostaju nepromijenjene.
            </p>

            <div className="row">
              <input
                ref={inputRef}
                className="input"
                type="text"
                value={draft}
                placeholder="Nova riječ"
                enterKeyHint="done"
                autoCapitalize="sentences"
                autoCorrect="off"
                onChange={(e) => {
                  setDraft(e.target.value);
                  if (error) setError(null);
                }}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    handleAdd();
                  }
                }}
                aria-label="Nova riječ"
              />
              <button className="btn btn--primary btn--sm" onClick={handleAdd}>
                Dodaj
              </button>
            </div>

            {error && <p className="notice">{error}</p>}

            {customWords.length === 0 ? (
              <p className="empty-note">Još nema tvojih riječi.</p>
            ) : (
              <ul className="word-list">
                {customWords.map((word) => (
                  <li className="word-list__item" key={word}>
                    <span className="word-list__text">{word}</span>
                    {pendingDelete === word ? (
                      <>
                        <button
                          className="confirm-btn"
                          onClick={() => {
                            onDeleteWord(word);
                            setPendingDelete(null);
                          }}
                        >
                          Obriši
                        </button>
                        <button
                          className="icon-btn"
                          onClick={() => setPendingDelete(null)}
                          aria-label="Odustani"
                        >
                          <svg
                            width="22"
                            height="22"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2"
                            strokeLinecap="round"
                            aria-hidden="true"
                          >
                            <line x1="18" y1="6" x2="6" y2="18" />
                            <line x1="6" y1="6" x2="18" y2="18" />
                          </svg>
                        </button>
                      </>
                    ) : (
                      <button
                        className="icon-btn icon-btn--danger"
                        onClick={() => setPendingDelete(word)}
                        aria-label={`Obriši riječ ${word}`}
                      >
                        <svg
                          width="22"
                          height="22"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          aria-hidden="true"
                        >
                          <polyline points="3 6 5 6 21 6" />
                          <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
                          <path d="M10 11v6M14 11v6" />
                          <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
                        </svg>
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </section>
    </main>
  );
}
