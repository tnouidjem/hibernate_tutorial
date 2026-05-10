package com.example.hibernatetutorial.tutorial.usecase;

import com.example.hibernatetutorial.domain.Product;
import com.example.hibernatetutorial.repository.ProductRepository;
import com.example.hibernatetutorial.tutorial.HibernateDiagnostics;
import com.example.hibernatetutorial.tutorial.TutorialConsole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.util.List;

import static com.example.hibernatetutorial.tutorial.usecase.UseCaseProductCodes.PHONE_INITIAL_PRICE;
import static com.example.hibernatetutorial.tutorial.usecase.UseCaseProductCodes.PHONE_PRODUCT_CODE;

@Component
public class UseCase04bCommitFlushModeBeforeQuery implements HibernateUseCase {

    private final EntityManager entityManager;
    private final ProductRepository productRepository;
    private final TutorialConsole console;
    private final HibernateDiagnostics diagnostics;

    public UseCase04bCommitFlushModeBeforeQuery(
            EntityManager entityManager,
            ProductRepository productRepository,
            TutorialConsole console,
            HibernateDiagnostics diagnostics
    ) {
        this.entityManager = entityManager;
        this.productRepository = productRepository;
        this.console = console;
        this.diagnostics = diagnostics;
    }

    /**
     * But de la demonstration: montrer que le mode FlushModeType.COMMIT peut reporter l'UPDATE.
     * Explication technique: Hibernate peut attendre la fin de transaction pour flusher. Une requete executee avant
     * le commit peut alors lire l'etat deja present en base, sans voir la modification en attente dans l'entite.
     */
    @Override
    @Transactional
    public void run() {
        console.title("4b. Flush COMMIT: une requete peut ne pas voir une modification en attente");
        diagnostics.resetStatistics();
        diagnostics.print("debut");

        // ETAPE 4b.1 - Marquer la transaction en rollback-only pour isoler la demonstration.
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

        // ETAPE 4b.2 - Basculer temporairement l'EntityManager en flush COMMIT.
        FlushModeType previousFlushMode = entityManager.getFlushMode();
        entityManager.setFlushMode(FlushModeType.COMMIT);
        try {
            // ETAPE 4b.3 - Charger puis modifier le smartphone sans flush immediat.
            Product phone = productRepository.findByProductCode(PHONE_PRODUCT_CODE).orElseThrow();
            phone.setPrice(new BigDecimal("1799.00"));
            diagnostics.print("apres modification smartphone");

            // ETAPE 4b.4 - Executer une requete avant commit: elle peut lire l'etat deja present en base.
            List<Product> expensiveProducts = productRepository.findProductsFromPrice(new BigDecimal("1500.00"));
            diagnostics.print("apres requete en flush COMMIT");

            // ETAPE 4b.5 - Afficher la difference de visibilite par rapport au flush AUTO.
            console.step("Regardez les logs SQL: le SELECT part sans UPDATE prealable du smartphone.");
            console.value("Smartphone present dans le resultat", expensiveProducts.stream().anyMatch(p -> PHONE_PRODUCT_CODE.equals(p.getProductCode())));
            console.step("La transaction est rollback-only pour eviter de conserver ce prix fictif.");
        } finally {
            // ETAPE 4b.6 - Restaurer le flush mode precedent pour ne pas affecter les autres demonstrations.
            entityManager.setFlushMode(previousFlushMode);
        }
    }

    @Override
    public void after() {
        Product phone = productRepository.findByProductCode(PHONE_PRODUCT_CODE).orElseThrow();

        console.step("Verification apres transaction 4b.");
        console.value("UPDATE Hibernate envoyes", diagnostics.entityUpdateCount());
        console.check("Aucun UPDATE envoye avant le rollback", diagnostics.entityUpdateCount() == 0);
        console.value("Prix relu en base", phone.getPrice());
        console.check("Rollback final applique", PHONE_INITIAL_PRICE.compareTo(phone.getPrice()) == 0);
    }
}
