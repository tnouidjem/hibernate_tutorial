package com.example.hibernatetutorial.tutorial;

import com.example.hibernatetutorial.domain.Product;
import com.example.hibernatetutorial.domain.PurchaseOrder;
import com.example.hibernatetutorial.dto.ProductSalesDto;
import com.example.hibernatetutorial.repository.ProductRepository;
import com.example.hibernatetutorial.repository.PurchaseOrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;

@Service
public class HibernateUseCases {

    private static final String LAPTOP_SKU = "SKU-LAPTOP-PRO";
    private static final String PHONE_SKU = "SKU-PHONE-PLUS";
    private static final String HEADSET_SKU = "SKU-HEADSET-BT";
    private static final String KEYBOARD_SKU = "SKU-KEYBOARD-MECH";

    private final EntityManager entityManager;
    private final ProductRepository productRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final TutorialConsole console;

    public HibernateUseCases(
            EntityManager entityManager,
            ProductRepository productRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            TutorialConsole console
    ) {
        this.entityManager = entityManager;
        this.productRepository = productRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.console = console;
    }

    /**
     * But de la demonstration: montrer le cas le plus direct ou le cache de premier niveau evite un deuxieme SELECT.
     * Explication technique: findById(...) delegue a EntityManager.find(...), qui consulte d'abord le Persistence
     * Context par cle primaire. Une fois l'entite chargee, le deuxieme findById(...) retourne l'instance managee
     * sans requete SQL supplementaire.
     */
    @Transactional
    public void demonstrateRepositoryFindByIdUsesFirstLevelCache() {
        console.title("0a. Repository findById: le deuxieme acces par id ne refait pas de SELECT");

        Long productId = productRepository.findIdBySku(LAPTOP_SKU);

        Product firstRepositoryLoad = productRepository.findById(productId).orElseThrow();
        Product secondRepositoryLoad = productRepository.findById(productId).orElseThrow();

        console.value("Meme instance Java", firstRepositoryLoad == secondRepositoryLoad);

        firstRepositoryLoad.setName("Ultrabook modifie via findById");

        console.value("Nom via premiere instance", firstRepositoryLoad.getName());
        console.value("Nom via seconde instance", secondRepositoryLoad.getName());
        console.step("Regardez les logs SQL: apres le SELECT par id initial, le deuxieme findById ne relit pas la ligne.");
        console.step("La transaction commit ensuite: le setter declenche un UPDATE par dirty checking.");
    }

    /**
     * But de la demonstration: montrer que deux appels repository successifs peuvent retourner la meme instance Java.
     * Explication technique: dans une transaction, Spring Data JPA utilise l'EntityManager lie a la transaction.
     * Une requete par critere comme findBySku(...) execute toutefois le SELECT a chaque appel, car le cache de
     * premier niveau est indexe par id. Hibernate reutilise ensuite l'instance deja managee pour cet id.
     */
    @Transactional
    public void demonstrateRepositoryCallsShareFirstLevelCache() {
        console.title("0b. Repository findBySku: deux SELECT, mais une seule instance Java");

        // Ces deux appels passent par Spring Data JPA, mais ils utilisent le meme EntityManager transactionnel.
        Product firstRepositoryLoad = productRepository.findBySku(LAPTOP_SKU).orElseThrow();
        Product secondRepositoryLoad = productRepository.findBySku(LAPTOP_SKU).orElseThrow();

        console.value("Meme instance Java", firstRepositoryLoad == secondRepositoryLoad);

        // Le setter modifie l'objet manage dans le Persistence Context. Comme les deux variables pointent vers
        // la meme instance Java, la seconde reference voit immediatement la modification faite sur la premiere.
        firstRepositoryLoad.setName("Ultrabook renomme dans le cache");

        console.value("Nom via premiere instance", firstRepositoryLoad.getName());
        console.value("Nom via seconde instance", secondRepositoryLoad.getName());
        console.step("A ce stade, aucun save(...) n'est necessaire pour que les deux references voient la meme valeur.");
        console.step("La transaction va commit: regardez les logs SQL, Hibernate fera un UPDATE a la fin de la methode.");
    }

    /**
     * But de la demonstration: montrer explicitement le cache de premier niveau avec EntityManager.find(...).
     * Explication technique: le Persistence Context garantit une seule instance Java pour une entite donnee
     * pendant la transaction. Apres EntityManager.clear(), cette garantie repart de zero et Hibernate doit relire
     * l'entite depuis la base.
     */
    @Transactional
    public void demonstrateFirstLevelCache() {
        console.title("1. Cache de premier niveau: une identite Java par entite dans une transaction");

        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

        Long productId = productRepository.findIdBySku(LAPTOP_SKU);

        // Premier acces: Hibernate doit executer un SELECT et stocke l'entite dans le Persistence Context.
        Product firstLoad = entityManager.find(Product.class, productId);

        // Deuxieme acces: pas de SELECT attendu, Hibernate retourne l'instance deja presente en cache de 1er niveau.
        Product secondLoad = entityManager.find(Product.class, productId);

        console.value("Meme instance Java avant clear()", firstLoad == secondLoad);
        console.value("EntityManager contient l'entite", entityManager.contains(firstLoad));

        // Les setters rendent visible un point important: dans la meme transaction, les deux variables pointent
        // vers la meme instance Java. Modifier firstLoad modifie donc aussi ce que voit secondLoad.
        firstLoad.setPrice(new BigDecimal("777.77"));
        console.value("Prix lu depuis firstLoad apres setter", firstLoad.getPrice());
        console.value("Prix lu depuis secondLoad", secondLoad.getPrice());

        entityManager.clear();

        // Apres clear(), le cache de 1er niveau est vide. Hibernate doit relire l'entite.
        // La modification precedente n'a pas ete flushee: clear() l'a detachee et elle ne sera pas persistee.
        Product afterClear = entityManager.find(Product.class, productId);
        console.value("Meme instance Java apres clear()", firstLoad == afterClear);
        console.value("Produit recharge", afterClear);
        console.step("La transaction est rollback-only pour que ce setter reste une demonstration sans effet durable.");
    }

    /**
     * But de la demonstration: montrer qu'une entite modifiee hors transaction de service n'est pas persistee.
     * Explication technique: l'appel repository ouvre puis ferme sa propre transaction. L'entite retournee devient
     * detachee; un setter change seulement l'objet Java en memoire, sans Persistence Context actif pour flusher.
     */
    public void demonstrateDetachedEntityOutsideServiceTransaction() {
        console.title("2a. Sans transaction de service: modification d'une entite detachee");

        Product headset = productRepository.findBySku(HEADSET_SKU).orElseThrow();
        BigDecimal originalPrice = headset.getPrice();

        // Le repository a ouvert puis ferme sa transaction read-only. L'entite retournee est detachee ici.
        headset.setPrice(new BigDecimal("1.00"));

        Product reloaded = productRepository.findBySku(HEADSET_SKU).orElseThrow();
        console.step("Le changement local n'est pas persiste, car aucun Persistence Context n'est actif autour du use case.");
        console.value("Prix modifie en memoire", headset.getPrice());
        console.value("Prix relu en base", reloaded.getPrice());
        console.value("Prix initial conserve", originalPrice.equals(reloaded.getPrice()));
    }

    /**
     * But de la demonstration: montrer qu'une transaction read-only n'est pas faite pour modifier des donnees.
     * Explication technique: Spring configure Hibernate en mode lecture, notamment avec un flush manual. Meme si
     * l'entite est modifiee en memoire et meme si save(...) est appele explicitement, Hibernate ne flush pas ce
     * changement au commit de cette transaction read-only.
     */
    @Transactional(readOnly = true)
    public void demonstrateDirtyCheckingInsideTransaction() {
        console.title("2b. Transaction read-only: setter + save explicite ne modifient pas la base");

        Session session = entityManager.unwrap(Session.class);
        Product keyboard = productRepository.findBySku(KEYBOARD_SKU).orElseThrow();
        BigDecimal originalPrice = keyboard.getPrice();
        BigDecimal newPrice = keyboard.getPrice().add(new BigDecimal("10.00"));

        // Le setter modifie bien l'objet Java en memoire.
        keyboard.setPrice(newPrice);

        // L'appel save(...) est volontairement present pour casser une idee recue: save ne force pas toujours
        // l'ecriture immediate en base. Ici, le contexte Hibernate reste configure pour une lecture.
        Product savedKeyboard = productRepository.save(keyboard);

        console.value("Transaction Spring read-only", TransactionSynchronizationManager.isCurrentTransactionReadOnly());
        console.value("Transaction Hibernate read-only", session.isDefaultReadOnly());
        console.value("Flush mode Hibernate", session.getHibernateFlushMode());
        console.value("Meme instance apres save(...)", keyboard == savedKeyboard);
        console.value("Prix initial", originalPrice);
        console.value("Prix modifie en memoire", savedKeyboard.getPrice());
        console.step("Regardez les logs SQL: aucun UPDATE du clavier n'est attendu au commit de cette transaction read-only.");
    }

    /**
     * But de la demonstration: verifier que le setter + save(...) en read-only n'ont pas ete commites.
     * Explication technique: cette lecture se fait apres la transaction read-only precedente. Si aucun UPDATE n'a
     * ete flush, la base contient encore le prix initial du clavier.
     */
    public void verifyReadOnlySaveWasNotCommitted() {
        Product keyboard = productRepository.findBySku(KEYBOARD_SKU).orElseThrow();
        console.value("Prix relu apres transaction read-only", keyboard.getPrice());
    }

    /**
     * But de la demonstration: montrer le piege de la self-invocation avec @Transactional.
     * Explication technique: Spring applique @Transactional via un proxy. Un appel a une methode de la meme classe
     * est un appel Java direct, donc il ne traverse pas le proxy et l'annotation transactionnelle n'est pas appliquee.
     */
    public void demonstrateTransactionalSelfInvocationHasNoEffect() {
        console.title("2c. Self-invocation: appeler une methode @Transactional de la meme classe ne passe pas par le proxy");

        Product headsetBefore = productRepository.findBySku(HEADSET_SKU).orElseThrow();
        BigDecimal originalPrice = headsetBefore.getPrice();

        // L'appel ci-dessous est un appel Java direct: this.method().
        // Spring ne peut pas intercepter cet appel avec son proxy transactionnel.
        methodAnnotatedTransactionalButCalledFromSameClass(new BigDecimal("2.00"));

        Product headsetAfter = productRepository.findBySku(HEADSET_SKU).orElseThrow();

        console.value("Prix avant self-invocation", originalPrice);
        console.value("Prix apres self-invocation", headsetAfter.getPrice());
        console.value("Modification persistee", headsetAfter.getPrice().compareTo(new BigDecimal("2.00")) == 0);
        console.step("Conclusion: l'annotation @Transactional n'a pas ete appliquee, car l'appel n'est pas passe par le proxy Spring.");
    }

    /**
     * But de la demonstration: servir de methode cible pour illustrer la self-invocation.
     * Explication technique: malgre l'annotation @Transactional, cette methode n'ouvre pas de transaction quand elle
     * est appelee depuis une autre methode de la meme instance; TransactionSynchronizationManager le rend visible.
     */
    @Transactional
    public void methodAnnotatedTransactionalButCalledFromSameClass(BigDecimal newPrice) {
        boolean transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
        console.value("Transaction active dans la methode annotee", transactionActive);

        Product headset = productRepository.findBySku(HEADSET_SKU).orElseThrow();

        // Comme l'annotation @Transactional n'est pas appliquee lors d'une self-invocation, le repository ouvre
        // puis ferme sa propre transaction. A cette ligne, l'entite est detachee: le setter ne sera pas flushe.
        headset.setPrice(newPrice);
        console.value("Prix modifie en memoire dans la methode annotee", headset.getPrice());
    }

    /**
     * But de la demonstration: montrer pourquoi les relations LAZY exigent une transaction ou une session ouverte.
     * Explication technique: la collection order.lines est un proxy Hibernate. Hors Persistence Context ouvert,
     * Hibernate ne peut plus executer le SELECT d'initialisation et leve une LazyInitializationException.
     */
    public void demonstrateLazyLoadingOutsideTransaction() {
        console.title("2d. Transaction et LAZY loading: acces hors transaction");

        PurchaseOrder order = purchaseOrderRepository.findFirstByOrderByIdAsc().orElseThrow();

        try {
            order.getLines().size();
        } catch (LazyInitializationException ex) {
            console.step("LazyInitializationException attendue: la collection LAZY est accedee apres fermeture de la transaction du repository.");
            console.value("Message Hibernate", ex.getMessage());
        }
    }

    /**
     * But de la demonstration: montrer que le LAZY loading fonctionne tant que la transaction reste ouverte.
     * Explication technique: la collection LAZY peut etre initialisee a la demande parce que l'EntityManager
     * transactionnel est encore lie au thread courant, meme si la transaction est en read-only.
     */
    @Transactional(readOnly = true)
    public void demonstrateLazyLoadingInsideTransaction() {
        console.title("2e. Transaction et LAZY loading: acces dans une transaction read-only");

        PurchaseOrder order = purchaseOrderRepository.findFirstByOrderByIdAsc().orElseThrow();

        // La transaction est encore ouverte: Hibernate peut initialiser la collection a la demande.
        console.value("Nombre de lignes commande", order.getLines().size());
        console.value("Montant total", order.getTotalAmount());
    }

    /**
     * But de la demonstration: montrer les DTO projections pour les lectures.
     * Explication technique: la requete JPQL construit directement des ProductSalesDto. Hibernate ne materialise
     * pas des entites Product completes, et la transaction read-only configure un mode de lecture plus adapte
     * avec un flush manual cote Hibernate.
     */
    @Transactional(readOnly = true)
    public void demonstrateDtoProjectionInReadOnlyTransaction() {
        console.title("3. Projection DTO dans une transaction read-only");

        Session session = entityManager.unwrap(Session.class);
        console.value("Transaction Hibernate read-only", session.isDefaultReadOnly());
        console.value("Flush mode Hibernate", session.getHibernateFlushMode());

        // La requete construit directement des DTO: inutile de charger des entites Product completes.
        List<ProductSalesDto> sales = productRepository.findSalesSummary();
        sales.forEach(dto -> console.value(dto.sku(), dto.quantitySold() + " ventes, CA " + dto.revenue() + " EUR"));
    }

    /**
     * But de la demonstration: montrer que le flush AUTO peut declencher un UPDATE avant un SELECT.
     * Explication technique: avant certaines requetes JPQL, Hibernate synchronise les changements en attente pour
     * que la requete lise un etat coherent avec le Persistence Context. Le SELECT voit donc la modification locale.
     */
    @Transactional
    public void demonstrateAutoFlushBeforeQuery() {
        console.title("4a. Flush AUTO: une requete JPQL peut declencher un UPDATE avant son SELECT");

        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

        Product laptop = productRepository.findBySku(LAPTOP_SKU).orElseThrow();
        laptop.setPrice(new BigDecimal("1999.00"));

        // En flush AUTO, Hibernate synchronise les changements en attente avant la requete JPQL.
        List<Product> expensiveProducts = productRepository.findProductsFromPrice(new BigDecimal("1500.00"));

        console.step("Regardez les logs SQL: l'UPDATE du laptop apparait avant le SELECT des produits chers.");
        console.value("Laptop present dans le resultat", expensiveProducts.stream().anyMatch(p -> LAPTOP_SKU.equals(p.getSku())));
        console.step("La transaction est marquee rollback-only pour garder les donnees initiales apres la demo.");
    }

    /**
     * But de la demonstration: montrer que le mode FlushModeType.COMMIT peut reporter l'UPDATE.
     * Explication technique: Hibernate peut attendre la fin de transaction pour flusher. Une requete executee avant
     * le commit peut alors lire l'etat deja present en base, sans voir la modification en attente dans l'entite.
     */
    @Transactional
    public void demonstrateCommitFlushModeBeforeQuery() {
        console.title("4b. Flush COMMIT: une requete peut ne pas voir une modification en attente");

        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

        FlushModeType previousFlushMode = entityManager.getFlushMode();
        entityManager.setFlushMode(FlushModeType.COMMIT);
        try {
            Product phone = productRepository.findBySku(PHONE_SKU).orElseThrow();
            phone.setPrice(new BigDecimal("1799.00"));

            // Avec FlushModeType.COMMIT, Hibernate peut reporter le flush. Le SELECT voit alors l'etat deja en base.
            List<Product> expensiveProducts = productRepository.findProductsFromPrice(new BigDecimal("1500.00"));

            console.step("Regardez les logs SQL: le SELECT part sans UPDATE prealable du smartphone.");
            console.value("Smartphone present dans le resultat", expensiveProducts.stream().anyMatch(p -> PHONE_SKU.equals(p.getSku())));
            console.step("La transaction est rollback-only pour eviter de conserver ce prix fictif.");
        } finally {
            entityManager.setFlushMode(previousFlushMode);
        }
    }
}
