/**
 * Shared API client. Backend base URL comes from env.
 * Session is an httpOnly cookie — never localStorage (XSS-safe).
 * All fetches send cookies (credentials: "include").
 */
const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";

export class ErreurApi extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export async function appelApi<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...init,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  });

  if (res.status === 401 && typeof window !== "undefined") {
    // Session expired: hard navigation so httpOnly cookie flow restarts at login.
    // (next/router not available in this plain helper — keep full reload.)
    window.location.assign("/connexion");
    throw new ErreurApi(401, "Session expirée — veuillez vous reconnecter.");
  }

  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new ErreurApi(res.status, body || `Erreur API (${res.status})`);
  }

  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

// Alias anglais pour compatibilité
export { ErreurApi as ApiError, appelApi as api };
