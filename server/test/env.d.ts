import type { D1Migration } from "cloudflare:test";

declare global {
  namespace Cloudflare {
    interface GlobalProps {
      mainModule: typeof import("../src/index");
    }
    interface Env {
      DB: D1Database;
      ADMIN_PIN?: string;
      TEST_MIGRATIONS: D1Migration[];
    }
  }
}
