package com.SecBank.pro.Models.Enums;

public enum StatusReclamation {
    OUVERTE,   // ancien statut : n'est plus attribué à la création
    EN_COURS,
    RESOLUE,
    FERMEE;

    /**
     * Transitions autorisées : EN_COURS vers RESOLUE ou FERMEE, RESOLUE vers FERMEE.
     * FERMEE est final. OUVERTE (ancien statut) peut seulement passer à EN_COURS.
     */
    public boolean peutPasserA(StatusReclamation cible) {
        return switch (this) {
            case OUVERTE -> cible == EN_COURS;
            case EN_COURS -> cible == RESOLUE || cible == FERMEE;
            case RESOLUE -> cible == FERMEE;
            case FERMEE -> false;
        };
    }
}
