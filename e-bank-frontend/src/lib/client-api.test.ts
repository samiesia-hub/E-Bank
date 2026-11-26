import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiError,
  BASE_API,
  ErreurApi,
  api,
  appelApi,
  messageErreurApi,
} from "./client-api";

const fetchMock = vi.fn();

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("ErreurApi", () => {
  it("est une Error qui porte le statut HTTP", () => {
    const erreur = new ErreurApi(404, "Introuvable");

    expect(erreur).toBeInstanceOf(Error);
    expect(erreur.status).toBe(404);
    expect(erreur.message).toBe("Introuvable");
  });
});

describe("appelApi", () => {
  it("appelle l'API avec les cookies et renvoie le JSON", async () => {
    fetchMock.mockResolvedValue({ ok: true, status: 200, json: async () => ({ id: 1 }) });

    const resultat = await appelApi<{ id: number }>("/comptes");

    expect(resultat).toEqual({ id: 1 });
    expect(fetchMock).toHaveBeenCalledWith(
      `${BASE_API}/comptes`,
      expect.objectContaining({
        credentials: "include",
        headers: expect.objectContaining({ "Content-Type": "application/json" }),
      }),
    );
  });

  it("fusionne les options et les en-têtes fournis", async () => {
    fetchMock.mockResolvedValue({ ok: true, status: 200, json: async () => ({}) });

    await appelApi("/virements", {
      method: "POST",
      headers: { Authorization: "Bearer jeton" },
    });

    expect(fetchMock).toHaveBeenCalledWith(
      `${BASE_API}/virements`,
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({
          Authorization: "Bearer jeton",
          "Content-Type": "application/json",
        }),
      }),
    );
  });

  it("renvoie undefined pour une réponse 204 sans lire le corps", async () => {
    const json = vi.fn();
    fetchMock.mockResolvedValue({ ok: true, status: 204, json });

    const resultat = await appelApi("/beneficiaires/3");

    expect(resultat).toBeUndefined();
    expect(json).not.toHaveBeenCalled();
  });

  it("lève une ErreurApi avec le corps de la réponse en cas d'échec", async () => {
    const corps = '{"message":"Solde insuffisant"}';
    fetchMock.mockResolvedValue({ ok: false, status: 400, text: async () => corps });

    const attente = appelApi("/virements");

    await expect(attente).rejects.toBeInstanceOf(ErreurApi);
    await expect(attente).rejects.toMatchObject({ status: 400, message: corps });
  });

  it("utilise un message générique quand le corps est vide", async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 500, text: async () => "" });

    await expect(appelApi("/x")).rejects.toMatchObject({
      status: 500,
      message: "Erreur API (500)",
    });
  });

  it("utilise un message générique quand le corps est illisible", async () => {
    fetchMock.mockResolvedValue({
      ok: false,
      status: 502,
      text: () => Promise.reject(new Error("flux interrompu")),
    });

    await expect(appelApi("/x")).rejects.toMatchObject({
      status: 502,
      message: "Erreur API (502)",
    });
  });

  it("signale une session expirée sur une réponse 401", async () => {
    // jsdom ne sait pas naviguer : il écrit une erreur "not implemented" qu'on fait taire.
    vi.spyOn(console, "error").mockImplementation(() => {});
    fetchMock.mockResolvedValue({ ok: false, status: 401, text: async () => "" });

    const attente = appelApi("/comptes");

    await expect(attente).rejects.toBeInstanceOf(ErreurApi);
    await expect(attente).rejects.toMatchObject({ status: 401 });
    await expect(attente).rejects.toThrow(/Session/);
  });
});

describe("messageErreurApi", () => {
  it("lit le champ message d'un refus métier", () => {
    const erreur = new ErreurApi(400, '{"message":"Solde insuffisant"}');

    expect(messageErreurApi(erreur)).toBe("Solde insuffisant");
  });

  it("lit les champs detail puis title d'un refus Spring", () => {
    expect(messageErreurApi(new ErreurApi(400, '{"detail":"Compte bloque"}'))).toBe(
      "Compte bloque",
    );
    expect(messageErreurApi(new ErreurApi(400, '{"title":"Bad Request"}'))).toBe("Bad Request");
  });

  it("donne la priorité à message, puis detail, puis title", () => {
    const corps = '{"title":"T","detail":"D","message":"M"}';

    expect(messageErreurApi(new ErreurApi(400, corps))).toBe("M");
    expect(messageErreurApi(new ErreurApi(400, '{"title":"T","detail":"D"}'))).toBe("D");
  });

  it("ignore les champs vides ou qui ne sont pas du texte", () => {
    expect(messageErreurApi(new ErreurApi(400, '{"message":"","detail":"Détail"}'))).toBe(
      "Détail",
    );
    expect(messageErreurApi(new ErreurApi(422, '{"foo":1}'))).toBe("Erreur API (422)");
  });

  it("renvoie le texte brut quand le corps n'est pas du JSON", () => {
    expect(messageErreurApi(new ErreurApi(503, "Service indisponible"))).toBe(
      "Service indisponible",
    );
  });

  it("masque un corps HTML derrière un message générique", () => {
    expect(messageErreurApi(new ErreurApi(502, "<html>Bad Gateway</html>"))).toBe(
      "Erreur API (502)",
    );
  });

  it("utilise un message générique quand le texte est vide", () => {
    expect(messageErreurApi(new ErreurApi(500, "   "))).toBe("Erreur API (500)");
  });

  it("explique un refus 403 sans message exploitable", () => {
    expect(messageErreurApi(new ErreurApi(403, "Forbidden"))).toMatch(/Vous n'avez pas/);
  });

  it("garde le message d'une Error ordinaire", () => {
    expect(messageErreurApi(new Error("Réseau coupé"))).toBe("Réseau coupé");
  });

  it("répond par un message par défaut pour une valeur inconnue", () => {
    expect(messageErreurApi("n'importe quoi")).toMatch(/Une erreur est survenue/);
    expect(messageErreurApi(undefined)).toMatch(/Une erreur est survenue/);
  });
});

describe("alias anglais", () => {
  it("pointent vers les mêmes exports", () => {
    expect(ApiError).toBe(ErreurApi);
    expect(api).toBe(appelApi);
  });
});
