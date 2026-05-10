package com.example.hibernatetutorial.tutorial.usecase;

import com.example.hibernatetutorial.domain.Product;
import com.example.hibernatetutorial.repository.ProductRepository;
import com.example.hibernatetutorial.tutorial.HibernateDiagnostics;
import com.example.hibernatetutorial.tutorial.TutorialConsole;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;

import static com.example.hibernatetutorial.tutorial.usecase.UseCaseProductCodes.LAPTOP_INITIAL_PRICE;
import static com.example.hibernatetutorial.tutorial.usecase.UseCaseProductCodes.LAPTOP_PRODUCT_CODE;

@Component
public class UseCase01FirstLevelCache implements HibernateUseCase {

    private final EntityManager entityManager;
    private final ProductRepository productRepository;
    private final TutorialConsole console;
    private final HibernateDiagnostics diagnostics;

    public UseCase01FirstLevelCache(
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
     * But de la demonstration: montrer explicitement le cache de premier niveau avec EntityManager.find(...).
     * Explication technique: le Persistence Context garantit une seule instance Java pour une entite donnee
     * pendant la transaction. Apres EntityManager.clear(), cette garantie repart de zero et Hibernate doit relire
     * l'entite depuis la base.
     */
    @Override
    @Transactional
    public void run() {
        console.title("1. Cache de premier niveau: une identite Java par entite dans une transaction");
        diagnostics.resetStatistics();
        diagnostics.print("debut");

        // ETAPE 1.1 - Marquer la transaction en rollback-only pour isoler la demonstration.
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

        // ETAPE 1.2 - Recuperer l'id du produit qui servira aux lectures EntityManager.
        Long productId = productRepository.findIdByProductCode(LAPTOP_PRODUCT_CODE);

        // ETAPE 1.3 - Premier acces: Hibernate execute un SELECT et stocke l'entite dans le Persistence Context.
        Product firstLoad = entityManager.find(Product.class, productId);
        diagnostics.print("apres premier EntityManager.find");

        // ETAPE 1.4 - Deuxieme acces: Hibernate retourne l'instance deja presente en cache de 1er niveau.
        Product secondLoad = entityManager.find(Product.class, productId);
        diagnostics.print("apres second EntityManager.find");

        // ETAPE 1.5 - Verifier l'identite Java et la presence de l'entite dans l'EntityManager.
        console.value("Meme instance Java avant clear()", firstLoad == secondLoad);
        console.value("EntityManager contient l'entite", entityManager.contains(firstLoad));

        // ETAPE 1.6 - Modifier l'entite managee pour observer la propagation entre references.
        firstLoad.setPrice(new BigDecimal("777.77"));
        console.value("Prix lu depuis firstLoad apres setter", firstLoad.getPrice());
        console.value("Prix lu depuis secondLoad", secondLoad.getPrice());

        // ETAPE 1.7 - Vider le Persistence Context pour detacher les instances courantes.
        entityManager.clear();
        diagnostics.print("apres clear");

        // ETAPE 1.8 - Relire apres clear(): Hibernate doit recreer une nouvelle instance depuis la base.
        Product afterClear = entityManager.find(Product.class, productId);
        diagnostics.print("apres relecture");
        console.value("Meme instance Java apres clear()", firstLoad == afterClear);
        console.value("Produit recharge", afterClear);
        console.step("La transaction est rollback-only pour que ce setter reste une demonstration sans effet durable.");
    }

    @Override
    public void after() {
        Product laptop = productRepository.findByProductCode(LAPTOP_PRODUCT_CODE).orElseThrow();

        console.step("Verification apres transaction 1.");
        console.value("UPDATE Hibernate envoyes", diagnostics.entityUpdateCount());
        console.check("Aucun UPDATE envoye apres clear()", diagnostics.entityUpdateCount() == 0);
        console.value("Prix relu en base", laptop.getPrice());
        console.check("Rollback/clear sans effet durable", LAPTOP_INITIAL_PRICE.compareTo(laptop.getPrice()) == 0);
    }
}
