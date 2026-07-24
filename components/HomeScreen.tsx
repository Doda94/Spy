"use client";

import { useRef, useState } from "react";
import type { Word } from "@/lib/words";

export type AddResult =
  | "ok"
  | "empty"
  | "too-long"
  | "duplicate"
  | "unauthorized"
  | "offline"
  | "error";
export type DeleteResult = "ok" | "not_found" | "unauthorized" | "offline" | "error";
export type UnlockResult =
  | "ok"
  | "unauthorized"
  | "pin_not_configured"
  | "offline"
  | "error";

export type WordsStatus = "loading" | "ready" | "offline";

type Props = {
  words: Word[];
  status: WordsStatus;
  unlocked: boolean;
  onUnlock: (pin: string) => Promise<UnlockResult>;
  onLock: () => void;
  onAddWord: (text: string) => Promise<AddResult>;
  onDeleteWord: (id: number) => Promise<DeleteResult>;
  onRetry: () => void;
  onPlay: () => void;
};

const ADD_MESSAGES: Record<Exclude<AddResult, "ok">, string> = {
  empty: "Upiši riječ prije dodavanja.",
  "too-long": "Riječ je predugačka.",
  duplicate: "Ta riječ već postoji na popisu.",
  unauthorized: "PIN više ne vrijedi. Otključaj ponovno.",
  offline: "Nema veze sa serverom. Riječ nije spremljena.",
  error: "Greška na serveru. Pokušaj ponovno.",
};

const DELETE_MESSAGES: Record<Exclude<DeleteResult, "ok">, string> = {
  not_found: "Ta je riječ već obrisana.",
  unauthorized: "PIN više ne vrijedi. Otključaj ponovno.",
  offline: "Nema veze sa serverom. Riječ nije obrisana.",
  error: "Greška na serveru. Pokušaj ponovno.",
};

const UNLOCK_MESSAGES: Record<Exclude<UnlockResult, "ok">, string> = {
  unauthorized: "Pogrešan PIN.",
  pin_not_configured: "PIN nije postavljen na serveru.",
  offline: "Nema veze sa serverom.",
  error: "Greška na serveru. Pokušaj ponovno.",
};

export default function HomeScreen({
  words,
  status,
  unlocked,
  onUnlock,
  onLock,
  onAddWord,
  onDeleteWord,
  onRetry,
  onPlay,
}: Props) {
  const [open, setOpen] = useState(false);
  const [wordsVisible, setWordsVisible] = useState(false);
  const [draft, setDraft] = useState("");
  const [pinDraft, setPinDraft] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<number | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  async function handleAdd() {
    if (busy) return;
    setBusy(true);
    const result = await onAddWord(draft);
    setBusy(false);
    if (result === "ok") {
      setMessage(null);
      setDraft("");
      inputRef.current?.focus();
    } else {
      setMessage(ADD_MESSAGES[result]);
    }
  }

  async function handleDelete(id: number) {
    if (busy) return;
    setBusy(true);
    const result = await onDeleteWord(id);
    setBusy(false);
    setPendingDelete(null);
    setMessage(result === "ok" ? null : DELETE_MESSAGES[result]);
  }

  async function handleUnlock() {
    if (busy) return;
    setBusy(true);
    const result = await onUnlock(pinDraft);
    setBusy(false);
    if (result === "ok") {
      setMessage(null);
      setPinDraft("");
    } else {
      setMessage(UNLOCK_MESSAGES[result]);
    }
  }

  function toggle() {
    setOpen((prev) => !prev);
    setMessage(null);
    setPendingDelete(null);
    setWordsVisible(false);
  }

  function toggleWordsVisible() {
    setWordsVisible((prev) => !prev);
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
          <span className="segmented__count">
            {status === "loading" ? "…" : words.length}
          </span>
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
            {status === "offline" ? (
              <div className="notice">
                <p style={{ margin: 0 }}>
                  Nema veze sa serverom — prikazane su zadnje spremljene riječi.
                </p>
                <button
                  className="btn btn--ghost btn--sm"
                  style={{ marginTop: 12 }}
                  onClick={onRetry}
                >
                  Pokušaj ponovno
                </button>
              </div>
            ) : (
              <p className="hint">
                Popis je zajednički za sve uređaje. Promjene vide svi igrači.
              </p>
            )}

            {unlocked ? (
              <>
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
                    disabled={busy}
                    onChange={(e) => {
                      setDraft(e.target.value);
                      if (message) setMessage(null);
                    }}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault();
                        void handleAdd();
                      }
                    }}
                    aria-label="Nova riječ"
                  />
                  <button
                    className="btn btn--primary btn--sm"
                    onClick={() => void handleAdd()}
                    disabled={busy}
                  >
                    Dodaj
                  </button>
                </div>
                <button className="btn btn--ghost btn--sm" onClick={onLock}>
                  Zaključaj uređivanje
                </button>
              </>
            ) : (
              <div className="row">
                <input
                  className="input"
                  type="password"
                  inputMode="numeric"
                  value={pinDraft}
                  placeholder="PIN za uređivanje"
                  enterKeyHint="go"
                  autoComplete="off"
                  disabled={busy || status === "offline"}
                  onChange={(e) => {
                    setPinDraft(e.target.value);
                    if (message) setMessage(null);
                  }}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      void handleUnlock();
                    }
                  }}
                  aria-label="PIN za uređivanje"
                />
                <button
                  className="btn btn--primary btn--sm"
                  onClick={() => void handleUnlock()}
                  disabled={busy || status === "offline"}
                >
                  Otključaj
                </button>
              </div>
            )}

            {message && <p className="notice">{message}</p>}

            <button
              className="btn btn--ghost btn--sm"
              onClick={toggleWordsVisible}
              aria-expanded={wordsVisible}
              aria-controls="popis-rijeci"
            >
              {wordsVisible
                ? "Sakrij riječi"
                : `Prikaži riječi (${status === "loading" ? "…" : words.length})`}
            </button>

            {wordsVisible && (
              <div id="popis-rijeci">
                {status === "loading" ? (
                  <p className="empty-note">Učitavanje riječi…</p>
                ) : words.length === 0 ? (
                  <p className="empty-note">Popis je prazan.</p>
                ) : (
                  <ul className="word-list">
                    {words.map((word) => (
                      <li className="word-list__item" key={word.id}>
                        <span className="word-list__text">{word.text}</span>
                        {!unlocked ? null : pendingDelete === word.id ? (
                          <>
                            <button
                              className="confirm-btn"
                              disabled={busy}
                              onClick={() => void handleDelete(word.id)}
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
                            onClick={() => setPendingDelete(word.id)}
                            aria-label={`Obriši riječ ${word.text}`}
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
          </div>
        )}
      </section>
    </main>
  );
}
