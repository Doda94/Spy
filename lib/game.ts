import type { Game, Settings } from "./types";

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
 * Nova igra: nasumična riječ s popisa i nasumično raspoređene uloge.
 * Redoslijed igrača ostaje 1..N, a nasumičnost dolazi iz miješanja uloga.
 */
export function createGame(settings: Settings, words: string[]): Game {
  const seats = shuffle(Array.from({ length: settings.playerCount }, (_, i) => i));
  return {
    playerCount: settings.playerCount,
    spyCount: settings.spyCount,
    word: pickRandom(words),
    spyIndices: seats.slice(0, settings.spyCount).sort((a, b) => a - b),
    currentPlayer: 0,
  };
}

export function isSpy(game: Game, playerIndex: number): boolean {
  return game.spyIndices.includes(playerIndex);
}
