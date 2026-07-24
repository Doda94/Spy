export type WordSource = "builtin" | "custom" | "all";

export type Phase = "home" | "setup" | "reveal" | "discussion";

export type Settings = {
  playerCount: number;
  spyCount: number;
  wordSource: WordSource;
};

export type Game = {
  playerCount: number;
  spyCount: number;
  /** Zapamćeno da se igra ponaša isto i nakon osvježavanja stranice. */
  wordSource: WordSource;
  word: string;
  /** Indeksi igrača (0-bazirani) koji su špijuni. */
  spyIndices: number[];
  /** Indeks igrača (0-bazirani) koji je trenutno na redu. */
  currentPlayer: number;
};

export type SavedState = {
  phase: Phase;
  settings: Settings;
  game: Game | null;
};

export const MIN_PLAYERS = 3;
export const MAX_PLAYERS = 12;

export const DEFAULT_SETTINGS: Settings = {
  playerCount: 4,
  spyCount: 1,
  wordSource: "builtin",
};
