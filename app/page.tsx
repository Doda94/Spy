"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import DiscussionScreen from "@/components/DiscussionScreen";
import HomeScreen, {
  type AddResult,
  type DeleteResult,
  type UnlockResult,
  type WordsStatus,
} from "@/components/HomeScreen";
import RevealScreen from "@/components/RevealScreen";
import SetupScreen from "@/components/SetupScreen";
import * as api from "@/lib/api";
import { createGame } from "@/lib/game";
import {
  clearPin,
  loadCachedWords,
  loadPin,
  loadState,
  saveCachedWords,
  savePin,
  saveState,
} from "@/lib/storage";
import { DEFAULT_SETTINGS, type Game, type Phase, type Settings } from "@/lib/types";
import { checkWord, type Word } from "@/lib/words";

function sortWords(words: Word[]): Word[] {
  return [...words].sort((a, b) => a.text.localeCompare(b.text, "hr"));
}

export default function Page() {
  const [hydrated, setHydrated] = useState(false);
  const [phase, setPhase] = useState<Phase>("home");
  const [settings, setSettings] = useState<Settings>(DEFAULT_SETTINGS);
  const [game, setGame] = useState<Game | null>(null);

  const [words, setWords] = useState<Word[]>([]);
  const [status, setStatus] = useState<WordsStatus>("loading");
  const [pin, setPin] = useState("");

  const refreshWords = useCallback(async () => {
    setStatus("loading");
    try {
      const fetched = sortWords(await api.fetchWords());
      setWords(fetched);
      saveCachedWords(fetched);
      setStatus("ready");
    } catch {
      // Bez mreže se igra i dalje može odigrati sa zadnjim spremljenim popisom.
      setWords(loadCachedWords());
      setStatus("offline");
    }
  }, []);

  // Učitavanje spremljenog stanja (samo na klijentu, jednom).
  useEffect(() => {
    const saved = loadState();
    if (saved) {
      setPhase(saved.phase);
      setSettings(saved.settings);
      setGame(saved.game);
    }
    setWords(loadCachedWords());
    setPin(loadPin());
    setHydrated(true);
    void refreshWords();
  }, [refreshWords]);

  // Spremanje stanja igre nakon svake promjene.
  useEffect(() => {
    if (!hydrated) return;
    saveState({ phase, settings, game });
  }, [hydrated, phase, settings, game]);

  const wordTexts = useMemo(() => words.map((w) => w.text), [words]);

  function lock() {
    clearPin();
    setPin("");
  }

  async function unlock(candidate: string): Promise<UnlockResult> {
    if (!candidate.trim()) return "unauthorized";
    try {
      await api.verifyPin(candidate);
      savePin(candidate);
      setPin(candidate);
      return "ok";
    } catch (error) {
      const code = error instanceof api.ApiError ? error.code : "server";
      if (code === "unauthorized" || code === "pin_not_configured" || code === "offline") {
        return code;
      }
      return "error";
    }
  }

  async function addWord(raw: string): Promise<AddResult> {
    const check = checkWord(raw);
    if (!check.ok) return check.reason;

    try {
      const created = await api.addWord(check.text, pin);
      const next = sortWords([...words, created]);
      setWords(next);
      saveCachedWords(next);
      return "ok";
    } catch (error) {
      const code = error instanceof api.ApiError ? error.code : "server";
      if (code === "unauthorized") lock();
      if (code === "duplicate" || code === "unauthorized" || code === "offline") {
        return code;
      }
      return "error";
    }
  }

  async function deleteWord(id: number): Promise<DeleteResult> {
    try {
      await api.deleteWord(id, pin);
    } catch (error) {
      const code = error instanceof api.ApiError ? error.code : "server";
      if (code === "unauthorized") {
        lock();
        return "unauthorized";
      }
      if (code === "offline") return "offline";
      if (code !== "not_found") return "error";
      // "not_found" znači da je riječ već nestala — lokalno je svejedno miči.
    }
    const remaining = words.filter((w) => w.id !== id);
    setWords(remaining);
    saveCachedWords(remaining);
    return "ok";
  }

  function startGame() {
    if (wordTexts.length === 0) return;
    setGame(createGame(settings, wordTexts));
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
    return <RevealScreen key={game.currentPlayer} game={game} onNext={nextPlayer} />;
  }

  if (phase === "discussion" && game) {
    return <DiscussionScreen spyCount={game.spyCount} onNewGame={newGame} />;
  }

  if (phase === "setup") {
    return (
      <SetupScreen
        settings={settings}
        onChange={setSettings}
        wordCount={words.length}
        status={status}
        onStart={startGame}
        onBack={() => setPhase("home")}
      />
    );
  }

  return (
    <HomeScreen
      words={words}
      status={status}
      unlocked={pin !== ""}
      onUnlock={unlock}
      onLock={lock}
      onAddWord={addWord}
      onDeleteWord={deleteWord}
      onRetry={() => void refreshWords()}
      onPlay={() => setPhase("setup")}
    />
  );
}
