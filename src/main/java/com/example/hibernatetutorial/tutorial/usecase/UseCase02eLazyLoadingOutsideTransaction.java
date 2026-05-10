package com.example.hibernatetutorial.tutorial.usecase;

import com.example.hibernatetutorial.domain.PurchaseOrder;
import com.example.hibernatetutorial.repository.PurchaseOrderRepository;
import com.example.hibernatetutorial.tutorial.HibernateDiagnostics;
import com.example.hibernatetutorial.tutorial.TutorialConsole;
import org.hibernate.LazyInitializationException;
import org.springframework.stereotype.Component;

@Component
public class UseCase02eLazyLoadingOutsideTransaction implements HibernateUseCase {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final TutorialConsole console;
    private final HibernateDiagnostics diagnostics;

    public UseCase02eLazyLoadingOutsideTransaction(
            PurchaseOrderRepository purchaseOrderRepository,
            TutorialConsole console,
            HibernateDiagnostics diagnostics
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.console = console;
        this.diagnostics = diagnostics;
    }

    /**
     * But de la demonstration: montrer pourquoi les relations LAZY exigent une transaction ou une session ouverte.
     * Explication technique: la collection order.lines est un proxy Hibernate. Hors Persistence Context ouvert,
     * Hibernate ne peut plus executer le SELECT d'initialisation et leve une LazyInitializationException.
     */
    @Override
    public void run() {
        console.title("2d. Transaction et LAZY loading: acces hors transaction");
        diagnostics.resetStatistics();
        diagnostics.print("debut");

        // ETAPE 2d.1 - Charger une commande sans transaction de service ouverte autour du use case.
        PurchaseOrder order = purchaseOrderRepository.findFirstByOrderByIdAsc().orElseThrow();
        diagnostics.print("apres lecture commande");

        // ETAPE 2d.2 - Acceder a la collection LAZY apres fermeture du Persistence Context.
        try {
            order.getLines().size();
        } catch (LazyInitializationException ex) {
            // ETAPE 2d.3 - Capturer l'exception attendue et afficher son message.
            console.step("LazyInitializationException attendue: la collection LAZY est accedee apres fermeture de la transaction du repository.");
            console.value("Message Hibernate", ex.getMessage());
        }
        diagnostics.print("fin");
    }

    @Override
    public void after() {
        console.step("Verification apres use case 2d.");
        console.value("UPDATE Hibernate envoyes", diagnostics.entityUpdateCount());
        console.check("Aucun UPDATE envoye par un test de LAZY loading", diagnostics.entityUpdateCount() == 0);
        console.check("La base reste lisible apres l'exception attendue", purchaseOrderRepository.findFirstByOrderByIdAsc().isPresent());
    }
}
