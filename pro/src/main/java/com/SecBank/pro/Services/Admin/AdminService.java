package Services.Admin;
public interface AdminService{
    void gererUtilisateurs();
    void activerDesactiverCompte(Long utilisateurId);
    void gererRolesEtPermissions();
    void gererEmployesSupport();
}