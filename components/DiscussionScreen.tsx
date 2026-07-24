"use client";

type Props = {
  spyCount: number;
  category: string;
  onNewGame: () => void;
};

/** "je 1 špijun", "su 3 špijuna", "je 5 špijuna" */
function spyPhrase(n: number): string {
  if (n === 1) return "je 1 špijun";
  if (n >= 2 && n <= 4) return `su ${n} špijuna`;
  return `je ${n} špijuna`;
}

export default function DiscussionScreen({ spyCount, category, onNewGame }: Props) {
  return (
    <main className="screen screen--center">
      <div className="spacer" />

      <span style={{ fontSize: 64, lineHeight: 1 }} aria-hidden="true">
        🕵️
      </span>

      <h1 className="screen-title">Rasprava je počela</h1>
      <p className="subtitle">
        Razgovarajte i glasajte za špijuna! Među vama {spyPhrase(spyCount)}.
      </p>
      <p className="player-badge" style={{ marginTop: 4 }}>
        Kategorija: {category}
      </p>

      <div className="spacer" />

      <button className="btn btn--primary" onClick={onNewGame}>
        Nova igra
      </button>
    </main>
  );
}
