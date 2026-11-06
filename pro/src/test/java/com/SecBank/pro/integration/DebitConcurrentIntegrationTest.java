package com.SecBank.pro.integration;

import com.SecBank.pro.Exceptions.OperationRefuseeException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Enums.Role;
import com.SecBank.pro.Models.Enums.StatutCompte;
import com.SecBank.pro.Models.Enums.TypeCompte;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.CompteBancaireRepository;
import com.SecBank.pro.Repositories.UserRepo;
import com.SecBank.pro.Services.Clients.CompteService;
import com.SecBank.pro.Utils.IbanGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrence reelle sur le solde : plusieurs operations simultanees sur le meme
 * compte (le chemin atomique qu'utilise un virement) ne doivent jamais produire
 * de mise a jour perdue ni de solde negatif.
 *
 * Le service de virement de ce module est encore un stub ; c'est donc la brique
 * {@link CompteService#debiter(Long, BigDecimal)} / {@code crediter} qui est
 * verifiee ici, avec de vrais threads et de vraies transactions.
 */
@SpringBootTest
class DebitConcurrentIntegrationTest {

    private static final int THREADS = 10;

    @Autowired
    private CompteService compteService;

    @Autowired
    private CompteBancaireRepository compteBancaireRepository;

    @Autowired
    private ClientRepo clientRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void viderLaBase() {
        compteBancaireRepository.deleteAll();
        clientRepo.deleteAll();
        userRepo.deleteAll();
    }

    @Test
    @DisplayName("Given 100 MAD et 10 retraits concurrents de 25, Then 4 reussissent et le solde tombe a 0.00")
    void retraitsConcurrentsNeFontJamaisPasserLeSoldeSousZero() throws Exception {
        CompteBancaire compte = creerCompte(new BigDecimal("100.00"));

        Resultat resultat = executerEnParallele(THREADS,
                () -> {
                    compteService.debiter(compte.getId(), new BigDecimal("25.00"));
                    return true;
                });

        assertEquals(4, resultat.succes(), "seuls 4 retraits de 25 sont couverts par 100 MAD");
        assertEquals(6, resultat.refus(), "les 6 autres doivent etre refuses proprement (400, pas 500)");
        assertEquals(0, resultat.autres());

        BigDecimal soldeFinal = compteBancaireRepository.findById(compte.getId())
                .orElseThrow().getSolde();
        assertEquals(0, soldeFinal.compareTo(new BigDecimal("0.00")),
                "aucune mise a jour perdue : solde final " + soldeFinal.toPlainString());
    }

    @Test
    @DisplayName("Given 10 depots concurrents de 50, Then le solde final vaut exactement 500.00")
    void depotsConcurrentsNePerdentAucuneOperation() throws Exception {
        CompteBancaire compte = creerCompte(new BigDecimal("0.00"));

        Resultat resultat = executerEnParallele(THREADS,
                () -> {
                    compteService.crediter(compte.getId(), new BigDecimal("50.00"));
                    return true;
                });

        assertEquals(THREADS, resultat.succes());
        assertEquals(0, resultat.refus());

        BigDecimal soldeFinal = compteBancaireRepository.findById(compte.getId())
                .orElseThrow().getSolde();
        assertEquals(0, soldeFinal.compareTo(new BigDecimal("500.00")),
                "tous les credits doivent etre pris en compte : " + soldeFinal.toPlainString());
    }

    @Test
    @DisplayName("Given un compte bloque, Then les operations concurrentes sont toutes refusees et le solde inchange")
    void compteBloqueRefuseToutesLesOperationsConcurrentes() throws Exception {
        CompteBancaire compte = creerCompte(new BigDecimal("100.00"));
        compte.setStatutCompte(StatutCompte.BLOQUE);
        compteBancaireRepository.saveAndFlush(compte);

        Resultat resultat = executerEnParallele(THREADS,
                () -> {
                    compteService.debiter(compte.getId(), new BigDecimal("10.00"));
                    return true;
                });

        assertEquals(0, resultat.succes());
        assertEquals(THREADS, resultat.refus());

        BigDecimal soldeFinal = compteBancaireRepository.findById(compte.getId())
                .orElseThrow().getSolde();
        assertEquals(0, soldeFinal.compareTo(new BigDecimal("100.00")));
    }

    // --- Helpers ---------------------------------------------------------

    private record Resultat(int succes, int refus, int autres) {
    }

    /**
     * Lance les taches en parallele (barriere commune) et classe les issues :
     * succes, refus metier ({@link OperationRefuseeException}) ou autre erreur.
     */
    private Resultat executerEnParallele(int nombre, Callable<Boolean> tache) throws Exception {
        ExecutorService executeur = Executors.newFixedThreadPool(nombre);
        CountDownLatch depart = new CountDownLatch(1);
        AtomicInteger succes = new AtomicInteger();
        AtomicInteger refus = new AtomicInteger();
        AtomicInteger autres = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < nombre; i++) {
                futures.add(executeur.submit(() -> {
                    depart.await();
                    try {
                        tache.call();
                        succes.incrementAndGet();
                    } catch (OperationRefuseeException refusMetier) {
                        refus.incrementAndGet();
                    } catch (Exception autre) {
                        autres.incrementAndGet();
                    }
                    return null;
                }));
            }
            depart.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executeur.shutdownNow();
        }

        assertTrue(autres.get() == 0, "aucune erreur technique attendue, obtenu : " + autres.get());
        return new Resultat(succes.get(), refus.get(), autres.get());
    }

    private CompteBancaire creerCompte(BigDecimal solde) {
        Client client = new Client();
        client.setEmail("concurrence@exemple.test");
        client.setUsername("concurrence@exemple.test");
        client.setNom("Client Concurrence");
        client.setPassword(passwordEncoder.encode("Secret123"));
        client.setRole(Role.CLIENT);
        client.setActif(true);
        client.setNumeroClient("CLI-CONC-01");
        Client enregistre = clientRepo.save(client);

        String numeroCompte = IbanGenerator.genererNumeroCompte();
        CompteBancaire compte = new CompteBancaire();
        compte.setNumeroCompte(numeroCompte);
        compte.setRib(IbanGenerator.genererRib(numeroCompte));
        compte.setDevise("MAD");
        compte.setSolde(solde);
        compte.setTypeCompte(TypeCompte.COURANT);
        compte.setStatutCompte(StatutCompte.ACTIF);
        compte.setDateCreation(LocalDateTime.now());
        compte.setClient(enregistre);
        return compteBancaireRepository.saveAndFlush(compte);
    }
}
