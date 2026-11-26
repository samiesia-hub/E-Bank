import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BanniereErreur } from "./BanniereErreur";

// On isole le composant de la couche API : seule la sortie du message nous intéresse.
vi.mock("@/lib/client-api", () => ({
  messageErreurApi: (erreur: unknown) =>
    erreur instanceof Error ? erreur.message : "Erreur inconnue",
}));

describe("BanniereErreur", () => {
  it("affiche le titre et le message de l'erreur dans une alerte", () => {
    render(<BanniereErreur erreur={new Error("Serveur indisponible")} />);

    const alerte = screen.getByRole("alert");
    expect(alerte).toHaveTextContent("Une erreur est survenue");
    expect(alerte).toHaveTextContent("Serveur indisponible");
  });

  it("affiche un message par défaut quand l'erreur n'est pas une Error", () => {
    render(<BanniereErreur erreur="n'importe quoi" />);

    expect(screen.getByRole("alert")).toHaveTextContent("Erreur inconnue");
  });

  it("n'affiche pas de bouton quand reessayer n'est pas fourni", () => {
    render(<BanniereErreur erreur={new Error("Panne")} />);

    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("appelle reessayer au clic sur le bouton", async () => {
    const reessayer = vi.fn();
    const user = userEvent.setup();

    render(<BanniereErreur erreur={new Error("Panne")} reessayer={reessayer} />);
    await user.click(screen.getByRole("button", { name: /essayer/i }));

    expect(reessayer).toHaveBeenCalledTimes(1);
  });
});
