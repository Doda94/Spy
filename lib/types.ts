export type Phase = "home" | "setup" | "reveal" | "discussion";

export type Settings = {
  playerCount: number;
  spyCount: number;
  /**
   * Odabrane kategorije. `null` znači "još nije birano" pa se igra sa svime
   * (i nove kategorije ulaze same). Prazno polje znači da je igrač namjerno
   * odznačio sve — tada igra ne može početi.
   */
  categories: string[] | null;
};

export type Game = {
  playerCount: number;
  spyCount: number;
  word: string;
  /** Kategorija izvučene riječi; špijun je vidi kao jedini trag. */
  category: string;
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
  categories: null,
};
