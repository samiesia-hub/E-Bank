import { describe, expect, it } from "vitest";
import { render } from "@testing-library/react";
import {
  SqueletteCarteCompte,
  SqueletteLigne,
  SqueletteTransaction,
} from "./Squelettes";

describe("Squelettes", () => {
  it("SqueletteLigne est masqué aux lecteurs d'écran et animé", () => {
    const { container } = render(<SqueletteLigne />);

    const bloc = container.firstElementChild;
    expect(bloc).toHaveAttribute("aria-hidden", "true");
    expect(bloc).toHaveClass("animate-pulse");
  });

  it("SqueletteLigne ajoute la classe personnalisée", () => {
    const { container } = render(<SqueletteLigne className="h-4 w-28" />);

    const bloc = container.firstElementChild;
    expect(bloc).toHaveClass("h-4", "w-28", "animate-pulse");
  });

  it("SqueletteCarteCompte affiche cinq blocs de chargement et reste masqué", () => {
    const { container } = render(<SqueletteCarteCompte />);

    expect(container.firstElementChild).toHaveAttribute("aria-hidden", "true");
    expect(container.querySelectorAll(".animate-pulse")).toHaveLength(5);
  });

  it("SqueletteTransaction affiche une ligne de liste avec quatre blocs", () => {
    const { container } = render(
      <ul>
        <SqueletteTransaction />
      </ul>,
    );

    const ligne = container.querySelector("li");
    expect(ligne).not.toBeNull();
    expect(ligne).toHaveAttribute("aria-hidden", "true");
    expect(container.querySelectorAll(".animate-pulse")).toHaveLength(4);
  });
});
