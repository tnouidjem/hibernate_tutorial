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
    private final HibernateDiagnostics diagnostics;

    public HibernateUseCases(
            EntityManager entityManager,
            ProductRepository productRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            TutorialConsole console,
            HibernateDiagnostics diagnostics
    ) {
        this.entityManager = entityManager;
        this.productRepository = productRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.console = console;
        this.diagnostics = diagnostics;
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
        diagnostics.print("debut");

        // ETAPE 0a.1 - Recuperer l'identifiant qui servira aux deux lectures par cle primaire.
        Long productId = productRepository.findIdBySku(LAPTOP_SKU);

        // ETAPE 0a.2 - Charger une premiere fois le produit depuis le repository.
        Product firstRepositoryLoad = productRepository.findById(productId).orElseThrow();
        diagnostics.print("apres premier findById");

        // ETAPE 0a.3 - Relire le meme id: le cache de premier niveau doit fournir la meme instance.
        Product secondRepositoryLoad = productRepository.findById(productId).orElseThrow();
        diagnostics.print("apres second findById");

        // ETAPE 0a.4 - Verifier l'identite Java des deux references.
        console.value("Meme instance Java", firstRepositoryLoad == secondRepositoryLoad);

        // ETAPE 0a.5 - Modifier l'entite managee pour rendre le dirty checking visible.
        firstRepositoryLoad.setName("Ultrabook modifie via findById");

        // ETAPE 0a.6 - Afficher l'effet de la modification sur les deux references.
        console.value("Nom via premiere instance", firstRepositoryLoad.getName());
        console.value("Nom via seconde instance", secondRepositoryLoad.getName());
        console.step("Regardez les logs SQL: apres le SELECT par id initial, le deuxieme findById ne relit pas la ligne.");
        console.step("La transaction commit ensuite: le setter declenche un UPDATE par dirty checking.");
        diagnostics.print("fin avant commit");
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
        diagnostics.print("debut");

        // ETAPE 0b.1 - Executer un premier appel Spring Data JPA par critere.
        Product firstRepositoryLoad = productRepository.findBySku(LAPTOP_SKU).orElseThrow();
        diagnostics.print("apres premier findBySku");

        // ETAPE 0b.2 - Executer le meme appel: le SELECT repart, mais Hibernate reutilise l'instance managee.
        Product secondRepositoryLoad = productRepository.findBySku(LAPTOP_SKU).orElseThrow();
        diagnostics.print("apres second findBySku");

        // ETAPE 0b.3 - Confirmer que les deux variables pointent vers la meme instance Java.
        console.value("Meme instance Java", firstRepositoryLoad == secondRepositoryLoad);

        // ETAPE 0b.4 - Modifier l'objet manage dans le Persistence Context.
        firstRepositoryLoad.setName("Ultrabook renomme dans le cache");

        // ETAPE 0b.5 - Afficher la valeur via les deux references pour montrer l'effet partage.
        console.value("Nom via premiere instance", firstRepositoryLoad.getName());
        console.value("Nom via seconde instance", secondRepositoryLoad.getName());
        console.step("A ce stade, aucun save(...) n'est necessaire pour que les deux references voient la meme valeur.");
        console.step("La transaction va commit: regardez les logs SQL, Hibernate fera un UPDATE a la fin de la methode.");
        diagnostics.print("fin avant commit");
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
        diagnostics.print("debut");

        // ETAPE 1.1 - Marquer la transaction en rollback-only pour isoler la demonstration.
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

        // ETAPE 1.2 - Recuperer l'id du produit qui servira aux lectures EntityManager.
        Long productId = productRepository.findIdBySku(LAPTOP_SKU);

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

    /**
     * But de la demonstration: montrer qu'une entite modifiee hors transaction de service n'est pas persistee.
     * Explication technique: l'appel repository ouvre puis ferme sa propre transaction. L'entite retournee devient
     * detachee; un setter change seulement l'objet Java en memoire, sans Persistence Context actif pour flusher.
     */
    public void demonstrateDetachedEntityOutsideServiceTransaction() {
        console.title("2a. Sans transaction de service: modification d'une entite detachee");
        diagnostics.print("debut");

        // ETAPE 2a.1 - Charger le produit via le repository sans transaction de service englobante.
        Product headset = productRepository.findBySku(HEADSET_SKU).orElseThrow();
        diagnostics.print("apres appel repository");
        BigDecimal originalPrice = headset.getPrice();

        // ETAPE 2a.2 - Modifier l'entite detachee en memoire.
        headset.setPrice(new BigDecimal("1.00"));

        // ETAPE 2a.3 - Relire le produit depuis la base pour comparer avec l'objet modifie en memoire.
        Product reloaded = productRepository.findBySku(HEADSET_SKU).orElseThrow();

        // ETAPE 2a.4 - Afficher la preuve que la modification locale n'a pas ete persistee.
        console.step("Le changement local n'est pas persiste, car aucun Persistence Context n'est actif autour du use case.");
        console.value("Prix modifie en memoire", headset.getPrice());
        console.value("Prix relu en base", reloaded.getPrice());
        console.value("Prix initial conserve", originalPrice.equals(reloaded.getPrice()));
        diagnostics.print("fin");
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
        diagnostics.print("debut");

        // ETAPE 2b.1 - Recuperer la session Hibernate pour exposer l'etat read-only.
        Session session = entityManager.unwrap(Session.class);

        // ETAPE 2b.2 - Charger le clavier et memoriser son prix initial.
        Product keyboard = productRepository.findBySku(KEYBOARD_SKU).orElseThrow();
        diagnostics.print("apres lecture clavier");
        BigDecimal originalPrice = keyboard.getPrice();
        BigDecimal newPrice = keyboard.getPrice().add(new BigDecimal("10.00"));

        // ETAPE 2b.3 - Modifier l'objet Java en memoire.
        keyboard.setPrice(newPrice);

        // ETAPE 2b.4 - Appeler save(...) explicitement dans une transaction configuree en lecture seule.
        Product savedKeyboard = productRepository.save(keyboard);
        diagnostics.print("apres save explicite");

        // ETAPE 2b.5 - Afficher les indicateurs qui expliquent pourquoi aucun UPDATE n'est attendu.
        console.value("Transaction Spring read-only", TransactionSynchronizationManager.isCurrentTransactionReadOnly());
        console.value("Transaction Hibernate read-only", session.isDefaultReadOnly());
        console.value("Flush mode Hibernate", session.getHibernateFlushMode());
        console.value("Meme instance apres save(...)", keyboard == savedKeyboard);
        console.value("Prix initial", originalPrice);
        console.value("Prix modifie en memoire", savedKeyboard.getPrice());
        console.step("Regardez les logs SQL: aucun UPDATE du clavier n'est attendu au commit de cette transaction read-only.");
        diagnostics.print("fin avant commit read-only");
    }

    /**
     * But de la demonstration: verifier que le setter + save(...) en read-only n'ont pas ete commites.
     * Explication technique: cette lecture se fait apres la transaction read-only precedente. Si aucun UPDATE n'a
     * ete flush, la base contient encore le prix initial du clavier.
     */
    public void verifyReadOnlySaveWasNotCommitted() {
        console.title("2b-verification. Verification apres transaction read-only");
        diagnostics.print("debut");

        // ETAPE 2b-verification.1 - Relire le clavier apres la transaction read-only precedente.
        Product keyboard = productRepository.findBySku(KEYBOARD_SKU).orElseThrow();

        // ETAPE 2b-verification.2 - Afficher le prix effectivement conserve en base.
        console.value("Prix relu apres transaction read-only", keyboard.getPrice());
        diagnostics.print("fin");
    }

    /**
     * But de la demonstration: montrer le piege de la self-invocation avec @Transactional.
     * Explication technique: Spring applique @Transactional via un proxy. Un appel a une methode de la meme classe
     * est un appel Java direct, donc il ne traverse pas le proxy et l'annotation transactionnelle n'est pas appliquee.
     */
    public void demonstrateTransactionalSelfInvocationHasNoEffect() {
        console.title("2c. Self-invocation: appeler une methode @Transactional de la meme classe ne passe pas par le proxy");
        diagnostics.print("debut");

        // ETAPE 2c.1 - Lire le prix initial avant l'appel interne.
        Product headsetBefore = productRepository.findBySku(HEADSET_SKU).orElseThrow();
        BigDecimal originalPrice = headsetBefore.getPrice();

        // ETAPE 2c.2 - Appeler directement une methode annotee @Transactional de la meme classe.
        methodAnnotatedTransactionalButCalledFromSameClass(new BigDecimal("2.00"));

        // ETAPE 2c.3 - Relire le produit apres l'appel interne.
        Product headsetAfter = productRepository.findBySku(HEADSET_SKU).orElseThrow();

        // ETAPE 2c.4 - Comparer les prix pour montrer que la transaction annotee n'a pas ete appliquee.
        console.value("Prix avant self-invocation", originalPrice);
        console.value("Prix apres self-invocation", headsetAfter.getPrice());
        console.value("Modification persistee", headsetAfter.getPrice().compareTo(new BigDecimal("2.00")) == 0);
        console.step("Conclusion: l'annotation @Transactional n'a pas ete appliquee, car l'appel n'est pas passe par le proxy Spring.");
        diagnostics.print("fin");
    }

    /**
     * But de la demonstration: servir de methode cible pour illustrer la self-invocation.
     * Explication technique: malgre l'annotation @Transactional, cette methode n'ouvre pas de transaction quand elle
     * est appelee depuis une autre methode de la meme instance; TransactionSynchronizationManager le rend visible.
     */
    @Transactional
    public void methodAnnotatedTransactionalButCalledFromSameClass(BigDecimal newPrice) {
        diagnostics.print("debut methode annotee appelee directement");

        // ETAPE 2c-cible.1 - Verifier si une transaction est reellement active dans la methode cible.
        boolean transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
        console.value("Transaction active dans la methode annotee", transactionActive);

        // ETAPE 2c-cible.2 - Charger le produit depuis le repository appele par la methode cible.
        Product headset = productRepository.findBySku(HEADSET_SKU).orElseThrow();

        // ETAPE 2c-cible.3 - Modifier l'entite detachee: le setter ne sera pas flushe.
        headset.setPrice(newPrice);
        console.value("Prix modifie en memoire dans la methode annotee", headset.getPrice());
        diagnostics.print("fin methode annotee appelee directement");
    }

    /**
     * But de la demonstration: montrer pourquoi les relations LAZY exigent une transaction ou une session ouverte.
     * Explication technique: la collection order.lines est un proxy Hibernate. Hors Persistence Context ouvert,
     * Hibernate ne peut plus executer le SELECT d'initialisation et leve une LazyInitializationException.
     */
    public void demonstrateLazyLoadingOutsideTransaction() {
        console.title("2d. Transaction et LAZY loading: acces hors transaction");
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

    /**
     * But de la demonstration: montrer que le LAZY loading fonctionne tant que la transaction reste ouverte.
     * Explication technique: la collection LAZY peut etre initialisee a la demande parce que l'EntityManager
     * transactionnel est encore lie au thread courant, meme si la transaction est en read-only.
     */
    @Transactional(readOnly = true)
    public void demonstrateLazyLoadingInsideTransaction() {
        console.title("2e. Transaction et LAZY loading: acces dans une transaction read-only");
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

    /**
     * But de la demonstration: montrer les DTO projections pour les lectures.
     * Explication technique: la requete JPQL construit directement des ProductSalesDto. Hibernate ne materialise
     * pas des entites Product completes, et la transaction read-only configure un mode de lecture plus adapte
     * avec un flush manual cote Hibernate.
     */
    @Transactional(readOnly = true)
    public void demonstrateDtoProjectionInReadOnlyTransaction() {
        console.title("3. Projection DTO dans une transaction read-only");
        diagnostics.print("debut");

        // ETAPE 3.1 - Recuperer la session Hibernate pour afficher la configuration de lecture.
        Session session = entityManager.unwrap(Session.class);
        console.value("Transaction Hibernate read-only", session.isDefaultReadOnly());
        console.value("Flush mode Hibernate", session.getHibernateFlushMode());

        // ETAPE 3.2 - Executer la projection DTO sans materialiser des entites Product completes.
        List<ProductSalesDto> sales = productRepository.findSalesSummary();
        diagnostics.print("apres projection DTO");

        // ETAPE 3.3 - Afficher les resultats agreges retournes par la projection.
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
        diagnostics.print("debut");

        // ETAPE 4a.1 - Marquer la transaction en rollback-only pour ne pas garder le prix fictif.
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

        // ETAPE 4a.2 - Charger puis modifier le laptop dans le Persistence Context.
        Product laptop = productRepository.findBySku(LAPTOP_SKU).orElseThrow();
        laptop.setPrice(new BigDecimal("1999.00"));
        diagnostics.print("apres modification laptop");

        // ETAPE 4a.3 - Executer une requete JPQL qui peut declencher un flush AUTO avant son SELECT.
        List<Product> expensiveProducts = productRepository.findProductsFromPrice(new BigDecimal("1500.00"));
        diagnostics.print("apres requete qui declenche flush AUTO");

        // ETAPE 4a.4 - Verifier que la requete voit le laptop modifie avant le rollback final.
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
        diagnostics.print("debut");

        // ETAPE 4b.1 - Marquer la transaction en rollback-only pour isoler la demonstration.
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

        // ETAPE 4b.2 - Basculer temporairement l'EntityManager en flush COMMIT.
        FlushModeType previousFlushMode = entityManager.getFlushMode();
        entityManager.setFlushMode(FlushModeType.COMMIT);
        try {
            // ETAPE 4b.3 - Charger puis modifier le smartphone sans flush immediat.
            Product phone = productRepository.findBySku(PHONE_SKU).orElseThrow();
            phone.setPrice(new BigDecimal("1799.00"));
            diagnostics.print("apres modification smartphone");

            // ETAPE 4b.4 - Executer une requete avant commit: elle peut lire l'etat deja present en base.
            List<Product> expensiveProducts = productRepository.findProductsFromPrice(new BigDecimal("1500.00"));
            diagnostics.print("apres requete en flush COMMIT");

            // ETAPE 4b.5 - Afficher la difference de visibilite par rapport au flush AUTO.
            console.step("Regardez les logs SQL: le SELECT part sans UPDATE prealable du smartphone.");
            console.value("Smartphone present dans le resultat", expensiveProducts.stream().anyMatch(p -> PHONE_SKU.equals(p.getSku())));
            console.step("La transaction est rollback-only pour eviter de conserver ce prix fictif.");
        } finally {
            // ETAPE 4b.6 - Restaurer le flush mode precedent pour ne pas affecter les autres demonstrations.
            entityManager.setFlushMode(previousFlushMode);
        }
    }
}
