package com.example.hibernatetutorial.tutorial.usecase;

import com.example.hibernatetutorial.domain.Customer;
import com.example.hibernatetutorial.domain.OrderStatus;
import com.example.hibernatetutorial.domain.Product;
import com.example.hibernatetutorial.domain.PurchaseOrder;
import com.example.hibernatetutorial.tutorial.HibernateDiagnostics;
import com.example.hibernatetutorial.tutorial.TutorialConsole;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
public class UseCase04cPersistenceContextGrowthInLoop implements HibernateUseCase {

    private static final int ORDER_COUNT = 90;
    private static final int LINES_PER_ORDER = 4;
    private static final int MODIFIED_ENTITIES_PER_BATCH = 30;

    private final EntityManager entityManager;
    private final TutorialConsole console;
    private final HibernateDiagnostics diagnostics;

    public UseCase04cPersistenceContextGrowthInLoop(
            EntityManager entityManager,
            TutorialConsole console,
            HibernateDiagnostics diagnostics
    ) {
        this.entityManager = entityManager;
        this.console = console;
        this.diagnostics = diagnostics;
    }

    /**
     * But de la demonstration: montrer qu'une boucle de modification sur un graphe peut remplir le Persistence Context.
     * Explication technique: un client contient des commandes, et chaque commande contient des lignes. En parcourant
     * ce graphe pour modifier les commandes et leurs lignes, Hibernate garde chaque entite chargee ou modifiee dans
     * le Persistence Context jusqu'au clear(). Le flush synchronise les changements, mais ne detache pas les entites.
     */
    @Override
    @Transactional
    public void run() {
        console.title("4c. Persistence Context: modifier un graphe en boucle peut saturer le contexte");
        diagnostics.resetStatistics();
        diagnostics.print("debut");

        // ETAPE 4c.1 - Garder cette demonstration sans effet durable en base.
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

        // ETAPE 4c.2 - Preparer un graphe de donnees suffisamment grand pour rendre le probleme visible.
        Long customerId = createDemoGraph();
        entityManager.flush();
        entityManager.clear();
        diagnostics.print("apres preparation du graphe et clear initial");

        // ETAPE 4c.3 - Parcourir le graphe naturellement: Customer -> commandes -> lignes.
        Customer customer = entityManager.find(Customer.class, customerId);
        int modifiedOrders = 0;
        int modifiedLines = 0;
        int modifiedEntities = 0;
        int nextDiagnosticsThreshold = MODIFIED_ENTITIES_PER_BATCH;

        for (PurchaseOrder order : customer.getOrders()) {
            order.setStatus(OrderStatus.PAID);
            modifiedOrders++;
            modifiedEntities++;

            // ETAPE 4c.4 - Modifier les filles de filles: chaque commande charge et modifie ses lignes.
            for (var line : order.getLines()) {
                line.setQuantity(line.getQuantity() + 1);
                line.setUnitPriceSnapshot(line.getUnitPriceSnapshot().add(new BigDecimal("0.10")));
                modifiedLines++;
                modifiedEntities++;
            }

            if (modifiedEntities >= nextDiagnosticsThreshold) {
                diagnostics.print("sans clear - apres " + modifiedEntities + " entites modifiees");
                nextDiagnosticsThreshold += MODIFIED_ENTITIES_PER_BATCH;
            }
        }

        // ETAPE 4c.5 - Constater que la boucle a garde toutes les entites visitees dans le Persistence Context.
        console.value("Commandes modifiees sans clear", modifiedOrders);
        console.value("Lignes modifiees sans clear", modifiedLines);
        console.value("Entites modifiees sans clear", modifiedEntities);
        diagnostics.print("sans clear - fin de boucle avant flush");

        // ETAPE 4c.6 - Flusher ne suffit pas a alleger le contexte: les entites restent managees.
        entityManager.flush();
        diagnostics.print("sans clear - apres flush");

        // ETAPE 4c.7 - Clear detache enfin le graphe accumule.
        entityManager.clear();
        diagnostics.print("sans clear - apres clear");

        // ETAPE 4c.8 - Reprendre le traitement en chargeant les commandes une par une et en vidant par lots.
        List<Long> orderIds = findDemoOrderIds(customerId);
        int batchModifiedOrders = 0;
        int batchModifiedLines = 0;
        int batchModifiedEntities = 0;
        int totalModifiedEntitiesWithFlush = 0;

        for (Long orderId : orderIds) {
            PurchaseOrder order = findOrderWithLines(orderId);
            order.setStatus(OrderStatus.SHIPPED);
            batchModifiedOrders++;
            batchModifiedEntities++;
            totalModifiedEntitiesWithFlush++;

            for (var line : order.getLines()) {
                line.setQuantity(line.getQuantity() + 1);
                line.setUnitPriceSnapshot(line.getUnitPriceSnapshot().add(new BigDecimal("0.10")));
                batchModifiedLines++;
                batchModifiedEntities++;
                totalModifiedEntitiesWithFlush++;
            }

            if (batchModifiedEntities >= MODIFIED_ENTITIES_PER_BATCH) {
                diagnostics.print("avec flush+clear - avant flush");
                entityManager.flush();
                entityManager.clear();
                diagnostics.print("avec flush+clear - apres " + totalModifiedEntitiesWithFlush + " entites modifiees");
                batchModifiedEntities = 0;
            }
        }

        // ETAPE 4c.9 - Flusher et vider le dernier lot si la taille totale n'est pas un multiple exact du seuil.
        if (batchModifiedEntities > 0) {
            entityManager.flush();
            entityManager.clear();
        }
        diagnostics.print("avec flush+clear - fin de boucle");

        console.value("Commandes modifiees avec flush+clear", batchModifiedOrders);
        console.value("Lignes modifiees avec flush+clear", batchModifiedLines);
        console.value("Entites modifiees avec flush+clear", totalModifiedEntitiesWithFlush);
        console.step("Dans la premiere boucle, le contexte grossit jusqu'a contenir tout le graphe visite.");
        console.step("Dans la seconde boucle, flush()+clear() toutes les 30 entites modifiees limite le nombre d'entites managees.");
        console.step("La transaction est rollback-only pour supprimer les donnees de demonstration au retour de la methode.");
    }

    @Override
    public void after() {
        Long demoCustomerCount = entityManager
                .createQuery("""
                        select count(c)
                        from Customer c
                        where c.email = :email
                        """, Long.class)
                .setParameter("email", "demo.persistence-context@example.com")
                .getSingleResult();
        Long demoProductCount = entityManager
                .createQuery("""
                        select count(p)
                        from Product p
                        where p.productCode like 'CODE-BULK-%'
                        """, Long.class)
                .getSingleResult();

        console.step("Verification apres transaction 4c.");
        console.value("UPDATE Hibernate envoyes", diagnostics.entityUpdateCount());
        console.check("Des UPDATE ont ete envoyes pendant les flush", diagnostics.entityUpdateCount() > 0);
        console.check("Client de demo rollback", demoCustomerCount == 0);
        console.check("Produits bulk rollback", demoProductCount == 0);
    }

    private Long createDemoGraph() {
        Customer customer = new Customer("Demo", "Contexte", "demo.persistence-context@example.com");
        entityManager.persist(customer);

        List<Product> products = List.of(
                new Product("CODE-BULK-01", "Produit bulk 1", new BigDecimal("10.00"), 10_000),
                new Product("CODE-BULK-02", "Produit bulk 2", new BigDecimal("20.00"), 10_000),
                new Product("CODE-BULK-03", "Produit bulk 3", new BigDecimal("30.00"), 10_000),
                new Product("CODE-BULK-04", "Produit bulk 4", new BigDecimal("40.00"), 10_000)
        );
        products.forEach(entityManager::persist);

        for (int orderIndex = 1; orderIndex <= ORDER_COUNT; orderIndex++) {
            PurchaseOrder order = new PurchaseOrder(customer, OrderStatus.CREATED, Instant.now());

            for (int lineIndex = 0; lineIndex < LINES_PER_ORDER; lineIndex++) {
                order.addLine(products.get(lineIndex), 1);
            }

            entityManager.persist(order);
        }

        console.value("Commandes preparees", ORDER_COUNT);
        console.value("Lignes preparees", ORDER_COUNT * LINES_PER_ORDER);
        diagnostics.print("apres creation du graphe");
        return customer.getId();
    }

    private List<Long> findDemoOrderIds(Long customerId) {
        return entityManager
                .createQuery("""
                        select o.id
                        from PurchaseOrder o
                        where o.customer.id = :customerId
                        order by o.id
                        """, Long.class)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    private PurchaseOrder findOrderWithLines(Long orderId) {
        return entityManager
                .createQuery("""
                        select distinct o
                        from PurchaseOrder o
                        left join fetch o.lines
                        where o.id = :orderId
                        """, PurchaseOrder.class)
                .setParameter("orderId", orderId)
                .getSingleResult();
    }
}
