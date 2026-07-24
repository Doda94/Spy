"use client";

import { useState } from "react";
import { isSpy } from "@/lib/game";
import type { Game } from "@/lib/types";

type Props = {
  game: Game;
  onNext: () => void;
};

export default function RevealScreen({ game, onNext }: Props) {
  const [revealed, setRevealed] = useState(false);

  const playerNumber = game.currentPlayer + 1;
  const spy = isSpy(game, game.currentPlayer);
  const isLast = playerNumber === game.playerCount;

  function handleNext() {
    // Prvo sakrij sadržaj, tek onda prebaci na sljedećeg igrača.
    setRevealed(false);
    onNext();
  }

  return (
    <main className="screen">
      <span className="player-badge">
        Igrač {playerNumber} / {game.playerCount}
      </span>

      <div
        className={`reveal-card${revealed ? " reveal-card--revealed" : ""}`}
        aria-live="polite"
      >
        {revealed ? (
          spy ? (
            <p className="reveal-card__word">Ti si špijun!</p>
          ) : (
            <>
              <p className="reveal-card__eyebrow">Tajna riječ</p>
              <p className="reveal-card__word">{game.word}</p>
            </>
          )
        ) : (
          <>
            <span className="reveal-card__lock" aria-hidden="true">
              🤫
            </span>
            <p className="reveal-card__eyebrow">Dodaj telefon igraču {playerNumber}</p>
            <p className="hint">Neka nitko drugi ne gleda u ekran.</p>
          </>
        )}
      </div>

      <div className="progress" aria-hidden="true">
        {Array.from({ length: game.playerCount }, (_, i) => (
          <span
            key={i}
            className={`progress__dot${i < game.currentPlayer ? " progress__dot--done" : ""}`}
          />
        ))}
      </div>

      {revealed ? (
        <button className="btn btn--primary fade-in" onClick={handleNext}>
          {isLast ? "Sakrij i započni raspravu" : "Sakrij i sljedeći igrač"}
        </button>
      ) : (
        <button className="btn btn--primary" onClick={() => setRevealed(true)}>
          Prikaži
        </button>
      )}
    </main>
  );
}
