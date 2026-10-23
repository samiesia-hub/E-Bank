package Services.Clients;
import java.util.Map;
public interface ReclamationService{
    Reclamation creerReclamation(
        Long clientId,
        String objet,
        String description
    );
    Reclamation suivreReclamation(Long reclamationId);
    ResponseReclamation repondreReclamation(
        Long reclamationId;
        String contenu
    );
    Map<String,Object> consulterInfosClient(Long clientId);
    
}
