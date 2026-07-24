import builtInWords from "@/data/words.json";
import type { Game, Settings, WordSource } from "./types";

export const BUILT_IN_WORDS: string[] = builtInWords as string[];

/** Riječi iz odabranog izvora, bez duplikata i praznih unosa. */
export function buildWordPool(source: WordSource, customWords: string[]): string[] {
  const parts =
    source === "builtin"
      ? BUILT_IN_WORDS
      : source === "custom"
        ? customWords
        : [...BUILT_IN_WORDS, ...customWords];

  const seen = new Set<string>();
  const pool: string[] = [];
  for (const raw of parts) {
    const word = raw.trim();
    if (!word) continue;
    const key = word.toLocaleLowerCase("hr");
    if (seen.has(key)) continue;
    seen.add(key);
    pool.push(word);
  }
  return pool;
}

function pickRandom<T>(items: T[]): T {
  return items[Math.floor(Math.random() * items.length)];
}

/** Fisher–Yates nad kopijom. */
function shuffle<T>(items: T[]): T[] {
  const out = [...items];
  for (let i = out.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [out[i], out[j]] = [out[j], out[i]];
  }
  return out;
}

/**
 * Nova igra: nasumična riječ iz bazena i nasumično raspoređene uloge.
 * Redoslijed igrača ostaje 1..N, a nasumičnost dolazi iz miješanja uloga.
 */
export function createGame(settings: Settings, pool: string[]): Game {
  const seats = shuffle(
    Array.from({ length: settings.playerCount }, (_, i) => i)
  );
  return {
    playerCount: settings.playerCount,
    spyCount: settings.spyCount,
    wordSource: settings.wordSource,
    word: pickRandom(pool),
    spyIndices: seats.slice(0, settings.spyCount).sort((a, b) => a - b),
    currentPlayer: 0,
  };
}

export function isSpy(game: Game, playerIndex: number): boolean {
  return game.spyIndices.includes(playerIndex);
}
