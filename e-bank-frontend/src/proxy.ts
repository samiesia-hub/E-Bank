import { NextResponse, type NextRequest } from "next/server";
import { jwtVerify } from "jose";

/**
 * Next.js 16: middleware.ts is deprecated → use proxy.ts (same level as app/).
 * UI-level guard only — backend MUST re-check roles on every request.
 *
 * Expected session cookie: "session" (httpOnly, JWT with { role }).
 * Routes use groups (public)/(auth)/(client)/(support)/(admin) which do NOT
 * appear in the URL — so we match on final paths: /dashboard, /admin/..., etc.
 */

const SECRET = new TextEncoder().encode(
  process.env.JWT_SECRET ?? "dev-only-secret-change-me"
);

const rules: Array<{ prefix: string; roles: string[] }> = [
  { prefix: "/admin", roles: ["ADMIN"] },
  { prefix: "/tickets", roles: ["AGENT_SUPPORT", "ADMIN"] },
  { prefix: "/tableau-de-bord", roles: ["CLIENT"] },
  { prefix: "/comptes", roles: ["CLIENT"] },
  { prefix: "/virements", roles: ["CLIENT"] },
  { prefix: "/beneficiaires", roles: ["CLIENT"] },
  { prefix: "/reclamations", roles: ["CLIENT"] },
  { prefix: "/profil", roles: ["CLIENT", "AGENT_SUPPORT", "ADMIN"] },
];

export async function proxy(req: NextRequest) {
  const path = req.nextUrl.pathname;
  const rule = rules.find((r) => path === r.prefix || path.startsWith(r.prefix + "/"));
  if (!rule) return NextResponse.next();

  const token = req.cookies.get("session")?.value;
  if (!token) {
    return NextResponse.redirect(new URL("/connexion", req.url));
  }

  try {
    const { payload } = await jwtVerify(token, SECRET);
    const role = payload.role as string | undefined;
    if (!role || !rule.roles.includes(role)) {
      // Wrong role → send back to login (or a /403 page if you add one)
      return NextResponse.redirect(new URL("/connexion", req.url));
    }
    return NextResponse.next();
  } catch {
    return NextResponse.redirect(new URL("/connexion", req.url));
  }
}

export const config = {
  matcher: [
    "/tableau-de-bord/:path*",
    "/comptes/:path*",
    "/virements/:path*",
    "/beneficiaires/:path*",
    "/reclamations/:path*",
    "/profil/:path*",
    "/tickets/:path*",
    "/admin/:path*",
  ],
};
