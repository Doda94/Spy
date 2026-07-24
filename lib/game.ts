import type { Game, Settings } from "./types";
import { wordKey, type Word } from "./words";

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

/** Riječi iz odabranih kategorija. Prazan odabir znači "sve kategorije". */
export function wordsInCategories(words: Word[], categories: string[]): Word[] {
  if (categories.length === 0) return words;
  const wanted = new Set(categories.map(wordKey));
  return words.filter((word) => wanted.has(wordKey(word.category)));
}

/**
 * Nova igra: nasumična riječ iz odabranih kategorija i nasumično raspoređene
 * uloge. Redoslijed igrača ostaje 1..N, a nasumičnost dolazi iz miješanja uloga.
 */
export function createGame(settings: Settings, pool: Word[]): Game {
  const seats = shuffle(Array.from({ length: settings.playerCount }, (_, i) => i));
  const chosen = pickRandom(pool);
  return {
    playerCount: settings.playerCount,
    spyCount: settings.spyCount,
    word: chosen.text,
    category: chosen.category,
    spyIndices: seats.slice(0, settings.spyCount).sort((a, b) => a - b),
    currentPlayer: 0,
  };
}

export function isSpy(game: Game, playerIndex: number): boolean {
  return game.spyIndices.includes(playerIndex);
}
