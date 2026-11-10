/**
 * Parcours « mot de passe oublié » : la clé de stockage de l'e-mail entre
 * l'étape 1 (demande du code) et l'étape 2 (saisie du code).
 * sessionStorage seulement : rien ne survit à la fermeture de l'onglet.
 */
export const CLE_EMAIL_REINIT = "bankati.reinit.email";

/** Même règle que le backend (RegleMotDePasse) et que le formulaire d'inscription. */
export const REGLE_MOT_DE_PASSE =
  "12 caractères minimum, avec une majuscule, une minuscule et un chiffre";

export function motDePasseValide(motDePasse: string): boolean {
  return (
    motDePasse.length >= 12 &&
    new TextEncoder().encode(motDePasse).length <= 72 &&
    /[A-Z]/.test(motDePasse) &&
    /[a-z]/.test(motDePasse) &&
    /[0-9]/.test(motDePasse)
  );
}
