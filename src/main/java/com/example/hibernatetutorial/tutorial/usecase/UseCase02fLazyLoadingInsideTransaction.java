package com.example.hibernatetutorial.tutorial.usecase;

import com.example.hibernatetutorial.domain.PurchaseOrder;
import com.example.hibernatetutorial.repository.PurchaseOrderRepository;
import com.example.hibernatetutorial.tutorial.HibernateDiagnostics;
import com.example.hibernatetutorial.tutorial.TutorialConsole;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UseCase02fLazyLoadingInsideTransaction implements HibernateUseCase {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final TutorialConsole console;
    private final HibernateDiagnostics diagnostics;

    public UseCase02fLazyLoadingInsideTransaction(
            PurchaseOrderRepository purchaseOrderRepository,
            TutorialConsole console,
            HibernateDiagnostics diagnostics
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.console = console;
        this.diagnostics = diagnostics;
    }

    /**
     * But de la demonstration: montrer que le LAZY loading fonctionne tant que la transaction reste ouverte.
     * Explication technique: la collection LAZY peut etre initialisee a la demande parce que l'EntityManager
     * transactionnel est encore lie au thread courant, meme si la transaction est en read-only.
     */
    @Override
    @Transactional(readOnly = true)
    public void run() {
        console.title("2e. Transaction et LAZY loading: acces dans une transaction read-only");
        diagnostics.resetStatistics();
        diagnostics.print("debut");

        // ETAPE 2e.1 - Charger une commande pendant que la transaction read-only reste ouverte.
        PurchaseOrder order = purchaseOrderRepository.findFirstByOrderByIdAsc().orElseThrow();
        diagnostics.print("apres lecture commande");

        // ETAPE 2e.2 - Initialiser la collection LAZY a la demande dans la transaction active.
        console.value("Nombre de lignes commande", order.getLines().size());
        diagnostics.print("apres initialisation lignes LAZY");

        // ETAPE 2e.3 - Lire une donnee calculee apres initialisation des lignes.
        console.value("Montant total", order.getTotalAmount());
    }

    @Override
    public void after() {
        console.step("Verification apres transaction 2e.");
        console.value("UPDATE Hibernate envoyes", diagnostics.entityUpdateCount());
        console.check("Aucun UPDATE envoye pendant le LAZY loading read-only", diagnostics.entityUpdateCount() == 0);
        console.check("La base reste lisible apres la transaction", purchaseOrderRepository.findFirstByOrderByIdAsc().isPresent());
    }
}
