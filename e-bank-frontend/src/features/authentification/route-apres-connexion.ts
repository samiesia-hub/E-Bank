// Route d'arrivee apres connexion : chaque role a son espace (voir src/proxy.ts).
export function routeSelonRole(role?: string | null): string {
  if (role === "ADMIN") return "/admin";
  if (role === "AGENT_SUPPORT") return "/tickets";
  return "/tableau-de-bord";
}
