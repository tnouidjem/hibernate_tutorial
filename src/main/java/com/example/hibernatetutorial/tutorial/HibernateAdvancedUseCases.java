package com.example.hibernatetutorial.tutorial;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

@Service
public class HibernateAdvancedUseCases {

    private static final String PHONE_PRODUCT_CODE = "CODE-PHONE-PLUS";
    private static final String HEADSET_PRODUCT_CODE = "CODE-HEADSET-BT";
    private static final String MOUSE_PRODUCT_CODE = "CODE-MOUSE-WIRELESS";

    private final EntityManager entityManager;
    private final PlatformTransactionManager transactionManager;
    private final TutorialConsole console;
    private final HibernateDiagnostics diagnostics;

    public HibernateAdvancedUseCases(
            EntityManager entityManager,
            PlatformTransactionManager transactionManager,
            TutorialConsole console,
            HibernateDiagnostics diagnostics
    ) {
        this.entityManager = entityManager;
        this.transactionManager = transactionManager;
        this.console = console;
        this.diagnostics = diagnostics;
    }

    /**
     * But de la demonstration: comparer READ_COMMITTED et REPEATABLE_READ avec deux transactions concurrentes.
     * Explication technique: la transaction A lit un prix, la transaction B modifie et commit, puis A relit.
     * Selon le niveau d'isolation JDBC applique par Spring, A peut voir ou non le commit effectue par B.
     */
    public void demonstrateIsolationLevels() {
        console.title("5. Isolation: READ_COMMITTED vs REPEATABLE_READ avec deux transactions");
        diagnostics.print("debut");

        compareIsolationLevel(
                "READ_COMMITTED",
                TransactionDefinition.ISOLATION_READ_COMMITTED,
                new BigDecimal("59.90")
        );
        compareIsolationLevel(
                "REPEATABLE_READ",
                TransactionDefinition.ISOLATION_REPEATABLE_READ,
                new BigDecimal("69.90")
        );
        diagnostics.print("fin");
    }

    /**
     * But de la demonstration: factoriser le scenario d'isolation pour le rejouer avec plusieurs niveaux.
     * Explication technique: la lecture utilise un scalaire et non une entite, afin que le cache de premier niveau
     * ne masque pas le comportement reel du niveau d'isolation de la base.
     */
    private void compareIsolationLevel(String label, int isolationLevel, BigDecimal concurrentPrice) {
        BigDecimal originalPrice = readPriceInNewTransaction(MOUSE_PRODUCT_CODE);

        console.step("Isolation " + label + ": transaction A lit, transaction B modifie et commit, transaction A relit.");

        TransactionTemplate transactionA = new TransactionTemplate(transactionManager);
        transactionA.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionA.setIsolationLevel(isolationLevel);

        transactionA.execute(status -> {
            diagnostics.print(label + " - debut transaction A");
            BigDecimal firstRead = readPriceScalar(MOUSE_PRODUCT_CODE);
            diagnostics.print(label + " - apres premiere lecture scalaire");

            updatePriceInNewCommittedTransaction(MOUSE_PRODUCT_CODE, concurrentPrice);
            diagnostics.print(label + " - apres commit transaction B");

            // On lit un scalaire et non une entite. Sinon le cache de premier niveau pourrait masquer l'effet
            // du niveau d'isolation en retournant la meme instance deja presente dans le Persistence Context.
            BigDecimal secondRead = readPriceScalar(MOUSE_PRODUCT_CODE);
            diagnostics.print(label + " - apres seconde lecture scalaire");

            console.value(label + " - premiere lecture", firstRead);
            console.value(label + " - lecture apres commit externe", secondRead);
            console.value(label + " - la transaction A voit le commit externe", secondRead.compareTo(concurrentPrice) == 0);
            return null;
        });

        updatePriceInNewCommittedTransaction(MOUSE_PRODUCT_CODE, originalPrice);
    }

    /**
     * But de la demonstration: montrer que le cache de premier niveau peut cacher un changement commite ailleurs.
     * Explication technique: meme en READ_COMMITTED, si une entite est deja managee dans le Persistence Context,
     * Hibernate retourne cette instance. Il faut clear(), refresh() ou une nouvelle transaction pour revoir la base.
     */
    public void demonstrateFirstLevelCacheCanHideIsolationEffects() {
        console.title("6. Isolation et cache de premier niveau: une entite managee peut masquer un commit externe");
        diagnostics.print("debut");

        BigDecimal originalPrice = readPriceInNewTransaction(PHONE_PRODUCT_CODE);
        BigDecimal concurrentPrice = originalPrice.add(new BigDecimal("100.00"));

        TransactionTemplate transactionA = new TransactionTemplate(transactionManager);
        transactionA.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionA.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);

        transactionA.execute(status -> {
            diagnostics.print("debut transaction A");
            var firstEntity = entityManager
                    .createQuery("select p from Product p where p.productCode = :productCode", com.example.hibernatetutorial.domain.Product.class)
                    .setParameter("productCode", PHONE_PRODUCT_CODE)
                    .getSingleResult();
            diagnostics.print("apres premiere lecture entite");

            updatePriceInNewCommittedTransaction(PHONE_PRODUCT_CODE, concurrentPrice);
            diagnostics.print("apres commit externe");

            var secondEntity = entityManager
                    .createQuery("select p from Product p where p.productCode = :productCode", com.example.hibernatetutorial.domain.Product.class)
                    .setParameter("productCode", PHONE_PRODUCT_CODE)
                    .getSingleResult();
            diagnostics.print("apres seconde lecture entite");

            console.value("Meme instance Java", firstEntity == secondEntity);
            console.value("Prix vu sans clear()", secondEntity.getPrice());

            entityManager.clear();
            diagnostics.print("apres clear");

            var afterClear = entityManager
                    .createQuery("select p from Product p where p.productCode = :productCode", com.example.hibernatetutorial.domain.Product.class)
                    .setParameter("productCode", PHONE_PRODUCT_CODE)
                    .getSingleResult();
            diagnostics.print("apres relecture apres clear");

            console.value("Prix vu apres clear()", afterClear.getPrice());
            console.step("En READ_COMMITTED, la base peut etre a jour, mais une entite deja managee reste l'instance de reference.");
            return null;
        });

        updatePriceInNewCommittedTransaction(PHONE_PRODUCT_CODE, originalPrice);
        diagnostics.print("fin");
    }

    /**
     * But de la demonstration: montrer l'effet de PROPAGATION_REQUIRED sur un rollback interne.
     * Explication technique: REQUIRED rejoint la transaction existante. Quand le bloc interne marque rollback-only,
     * il marque en realite la transaction commune; le commit externe echoue avec UnexpectedRollbackException.
     */
    public void demonstrateRequiredPropagationRollback() {
        console.title("7. Propagation REQUIRED: l'appel interne participe a la transaction externe");
        diagnostics.print("debut");

        BigDecimal originalPrice = readPriceInNewTransaction(PHONE_PRODUCT_CODE);

        TransactionTemplate outerRequired = new TransactionTemplate(transactionManager);
        outerRequired.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        TransactionTemplate innerRequired = new TransactionTemplate(transactionManager);
        innerRequired.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        try {
            outerRequired.execute(outerStatus -> {
                diagnostics.print("debut transaction externe REQUIRED");
                updatePriceScalar(PHONE_PRODUCT_CODE, new BigDecimal("1500.00"));
                console.value("Prix dans la transaction externe", readPriceScalar(PHONE_PRODUCT_CODE));
                diagnostics.print("apres modification externe");

                innerRequired.execute(innerStatus -> {
                    diagnostics.print("debut appel interne REQUIRED");
                    updatePriceScalar(PHONE_PRODUCT_CODE, new BigDecimal("1600.00"));
                    console.value("Prix dans l'appel interne REQUIRED", readPriceScalar(PHONE_PRODUCT_CODE));
                    diagnostics.print("apres modification interne REQUIRED");

                    // REQUIRED ne cree pas une transaction separee ici. Marquer rollback-only impacte donc
                    // toute la transaction, pas seulement le bloc interne.
                    innerStatus.setRollbackOnly();
                    return null;
                });

                console.step("Le code externe continue, mais la transaction commune est rollback-only.");
                diagnostics.print("fin transaction externe avant rollback");
                return null;
            });
        } catch (UnexpectedRollbackException | TransactionSystemException ex) {
            console.value("Exception au commit", ex.getClass().getSimpleName());
        }

        BigDecimal priceAfterRollback = readPriceInNewTransaction(PHONE_PRODUCT_CODE);
        console.value("Prix apres rollback REQUIRED", priceAfterRollback);
        console.value("Prix initial conserve", priceAfterRollback.compareTo(originalPrice) == 0);
        diagnostics.print("fin");
    }

    /**
     * But de la demonstration: montrer qu'une transaction REQUIRES_NEW est independante de la transaction appelante.
     * Explication technique: Spring suspend la transaction externe, ouvre une nouvelle transaction pour le bloc
     * interne, puis la commit. Le rollback externe n'annule donc pas les changements deja commites en REQUIRES_NEW.
     */
    public void demonstrateRequiresNewPropagation() {
        console.title("8. Propagation REQUIRES_NEW: l'appel interne commit meme si l'externe rollback");
        diagnostics.print("debut");

        BigDecimal phoneOriginalPrice = readPriceInNewTransaction(PHONE_PRODUCT_CODE);
        BigDecimal headsetOriginalPrice = readPriceInNewTransaction(HEADSET_PRODUCT_CODE);

        TransactionTemplate outerRequired = new TransactionTemplate(transactionManager);
        outerRequired.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        TransactionTemplate innerRequiresNew = new TransactionTemplate(transactionManager);
        innerRequiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        outerRequired.execute(outerStatus -> {
            diagnostics.print("debut transaction externe REQUIRED");
            updatePriceScalar(PHONE_PRODUCT_CODE, new BigDecimal("1700.00"));
            console.value("Prix smartphone dans transaction externe", readPriceScalar(PHONE_PRODUCT_CODE));
            diagnostics.print("apres modification externe");

            innerRequiresNew.execute(innerStatus -> {
                diagnostics.print("debut transaction interne REQUIRES_NEW");
                updatePriceScalar(HEADSET_PRODUCT_CODE, new BigDecimal("80.00"));
                console.value("Prix casque dans REQUIRES_NEW", readPriceScalar(HEADSET_PRODUCT_CODE));
                diagnostics.print("fin transaction interne REQUIRES_NEW");
                return null;
            });
            diagnostics.print("apres retour REQUIRES_NEW");

            // Le rollback de la transaction externe n'annule pas le commit deja effectue dans REQUIRES_NEW.
            outerStatus.setRollbackOnly();
            diagnostics.print("fin transaction externe rollback-only");
            return null;
        });

        BigDecimal phoneAfterRollback = readPriceInNewTransaction(PHONE_PRODUCT_CODE);
        BigDecimal headsetAfterCommit = readPriceInNewTransaction(HEADSET_PRODUCT_CODE);

        console.value("Smartphone apres rollback externe", phoneAfterRollback);
        console.value("Casque apres commit REQUIRES_NEW", headsetAfterCommit);

        updatePriceInNewCommittedTransaction(HEADSET_PRODUCT_CODE, headsetOriginalPrice);
        updatePriceInNewCommittedTransaction(PHONE_PRODUCT_CODE, phoneOriginalPrice);
        diagnostics.print("fin");
    }

    private BigDecimal readPriceInNewTransaction(String productCode) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setReadOnly(true);
        return template.execute(status -> {
            diagnostics.print("lecture prix en REQUIRES_NEW read-only");
            return readPriceScalar(productCode);
        });
    }

    private void updatePriceInNewCommittedTransaction(String productCode, BigDecimal price) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.executeWithoutResult(status -> {
            diagnostics.print("update prix en REQUIRES_NEW");
            updatePriceScalar(productCode, price);
        });
    }

    private BigDecimal readPriceScalar(String productCode) {
        return entityManager
                .createQuery("select p.price from Product p where p.productCode = :productCode", BigDecimal.class)
                .setParameter("productCode", productCode)
                .getSingleResult();
    }

    private void updatePriceScalar(String productCode, BigDecimal price) {
        entityManager
                .createQuery("update Product p set p.price = :price where p.productCode = :productCode")
                .setParameter("price", price)
                .setParameter("productCode", productCode)
                .executeUpdate();
    }
}
