"use client";

import { useEffect, useMemo, useState } from "react";
import DiscussionScreen from "@/components/DiscussionScreen";
import HomeScreen from "@/components/HomeScreen";
import RevealScreen from "@/components/RevealScreen";
import SetupScreen from "@/components/SetupScreen";
import { buildWordPool, createGame } from "@/lib/game";
import {
  loadCustomWords,
  loadState,
  saveCustomWords,
  saveState,
} from "@/lib/storage";
import {
  DEFAULT_SETTINGS,
  type Game,
  type Phase,
  type Settings,
  type WordSource,
} from "@/lib/types";

export default function Page() {
  const [hydrated, setHydrated] = useState(false);
  const [customWords, setCustomWords] = useState<string[]>([]);
  const [phase, setPhase] = useState<Phase>("home");
  const [settings, setSettings] = useState<Settings>(DEFAULT_SETTINGS);
  const [game, setGame] = useState<Game | null>(null);

  // Učitavanje spremljenog stanja (samo na klijentu, jednom).
  useEffect(() => {
    setCustomWords(loadCustomWords());
    const saved = loadState();
    if (saved) {
      setPhase(saved.phase);
      setSettings(saved.settings);
      setGame(saved.game);
    }
    setHydrated(true);
  }, []);

  // Spremanje stanja igre nakon svake promjene.
  useEffect(() => {
    if (!hydrated) return;
    saveState({ phase, settings, game });
  }, [hydrated, phase, settings, game]);

  const poolSizes = useMemo<Record<WordSource, number>>(
    () => ({
      builtin: buildWordPool("builtin", customWords).length,
      custom: buildWordPool("custom", customWords).length,
      all: buildWordPool("all", customWords).length,
    }),
    [customWords]
  );

  function updateCustomWords(next: string[]) {
    setCustomWords(next);
    saveCustomWords(next);
  }

  function addWord(raw: string): "ok" | "empty" | "duplicate" {
    const word = raw.trim().replace(/\s+/g, " ");
    if (!word) return "empty";
    const key = word.toLocaleLowerCase("hr");
    if (customWords.some((w) => w.toLocaleLowerCase("hr") === key)) return "duplicate";
    updateCustomWords([...customWords, word]);
    return "ok";
  }

  function deleteWord(word: string) {
    updateCustomWords(customWords.filter((w) => w !== word));
  }

  function startGame() {
    const pool = buildWordPool(settings.wordSource, customWords);
    if (pool.length === 0) return;
    setGame(createGame(settings, pool));
    setPhase("reveal");
  }

  function nextPlayer() {
    if (!game) return;
    const next = game.currentPlayer + 1;
    if (next >= game.playerCount) {
      setPhase("discussion");
    } else {
      setGame({ ...game, currentPlayer: next });
    }
  }

  function newGame() {
    setGame(null);
    setPhase("setup");
  }

  // Do hidracije ne znamo pravu fazu — izbjegavamo bljesak krivog zaslona.
  if (!hydrated) return <main className="screen" />;

  if (phase === "reveal" && game) {
    return (
      <RevealScreen
        key={game.currentPlayer}
        game={game}
        onNext={nextPlayer}
      />
    );
  }

  if (phase === "discussion" && game) {
    return <DiscussionScreen spyCount={game.spyCount} onNewGame={newGame} />;
  }

  if (phase === "setup") {
    return (
      <SetupScreen
        settings={settings}
        onChange={setSettings}
        poolSizes={poolSizes}
        onStart={startGame}
        onBack={() => setPhase("home")}
      />
    );
  }

  return (
    <HomeScreen
      customWords={customWords}
      onAddWord={addWord}
      onDeleteWord={deleteWord}
      onPlay={() => setPhase("setup")}
    />
  );
}
