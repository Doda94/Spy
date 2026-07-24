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
import { createGame, wordsInCategories } from "@/lib/game";
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
import { categoriesOf, checkCategory, checkWord, wordKey, type Word } from "@/lib/words";

function sortWords(words: Word[]): Word[] {
  return [...words].sort(
    (a, b) =>
      a.category.localeCompare(b.category, "hr") || a.text.localeCompare(b.text, "hr")
  );
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

  const allCategories = useMemo(() => categoriesOf(words), [words]);

  // Kategorije koje su nestale (npr. zadnja riječ obrisana) ne smiju ostati
  // odabrane; prvi put, kad ništa nije birano, igra se sa svime.
  const selectedCategories = useMemo(() => {
    if (allCategories.length === 0) return [];
    if (settings.categories === null) return allCategories;
    const known = new Set(allCategories.map(wordKey));
    return settings.categories.filter((c) => known.has(wordKey(c)));
  }, [settings.categories, allCategories]);

  const categoryOptions = useMemo(
    () =>
      allCategories.map((name) => ({
        name,
        count: words.filter((w) => wordKey(w.category) === wordKey(name)).length,
      })),
    [allCategories, words]
  );

  const pool = useMemo(
    () => wordsInCategories(words, selectedCategories),
    [words, selectedCategories]
  );

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

  async function addWord(rawText: string, rawCategory: string): Promise<AddResult> {
    const word = checkWord(rawText);
    if (!word.ok) return word.reason;
    const category = checkCategory(rawCategory);
    if (!category.ok) return category.reason;

    // Ista riječ u drugoj kategoriji i dalje je duplikat — javi to bez čekanja.
    const key = wordKey(word.text);
    if (words.some((w) => wordKey(w.text) === key)) return "duplicate";

    try {
      const created = await api.addWord(word.text, category.text, pin);
      const next = sortWords([...words, created]);
      setWords(next);
      saveCachedWords(next);
      return "ok";
    } catch (error) {
      const code = error instanceof api.ApiError ? error.code : "server";
      if (code === "unauthorized") lock();
      if (
        code === "duplicate" ||
        code === "unauthorized" ||
        code === "offline" ||
        code === "category-empty" ||
        code === "category-too-long"
      ) {
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
    return <RevealScreen key={game.currentPlayer} game={game} onNext={nextPlayer} />;
  }

  if (phase === "discussion" && game) {
    return (
      <DiscussionScreen
        spyCount={game.spyCount}
        category={game.category}
        onNewGame={newGame}
      />
    );
  }

  if (phase === "setup") {
    return (
      <SetupScreen
        settings={{ ...settings, categories: selectedCategories }}
        onChange={setSettings}
        categories={categoryOptions}
        poolSize={pool.length}
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
