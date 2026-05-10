package com.example.hibernatetutorial.tutorial.usecase;

import com.example.hibernatetutorial.dto.ProductSalesDto;
import com.example.hibernatetutorial.repository.ProductRepository;
import com.example.hibernatetutorial.tutorial.HibernateDiagnostics;
import com.example.hibernatetutorial.tutorial.TutorialConsole;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class UseCase03DtoProjectionInReadOnlyTransaction implements HibernateUseCase {

    private final EntityManager entityManager;
    private final ProductRepository productRepository;
    private final TutorialConsole console;
    private final HibernateDiagnostics diagnostics;

    public UseCase03DtoProjectionInReadOnlyTransaction(
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
     * But de la demonstration: montrer les DTO projections pour les lectures.
     * Explication technique: la requete JPQL construit directement des ProductSalesDto. Hibernate ne materialise
     * pas des entites Product completes, et la transaction read-only configure un mode de lecture plus adapte
     * avec un flush manual cote Hibernate.
     */
    @Override
    @Transactional(readOnly = true)
    public void run() {
        console.title("3. Projection DTO dans une transaction read-only");
        diagnostics.resetStatistics();
        diagnostics.print("debut");

        // ETAPE 3.1 - Recuperer la session Hibernate pour afficher la configuration de lecture.
        Session session = entityManager.unwrap(Session.class);
        console.value("Transaction Hibernate read-only", session.isDefaultReadOnly());
        console.value("Flush mode Hibernate", session.getHibernateFlushMode());

        // ETAPE 3.2 - Executer la projection DTO sans materialiser des entites Product completes.
        List<ProductSalesDto> sales = productRepository.findSalesSummary();
        diagnostics.print("apres projection DTO");

        // ETAPE 3.3 - Afficher les resultats agreges retournes par la projection.
        sales.forEach(dto -> console.value(dto.productCode(), dto.quantitySold() + " ventes, CA " + dto.revenue() + " EUR"));
    }

    @Override
    public void after() {
        List<ProductSalesDto> sales = productRepository.findSalesSummary();

        console.step("Verification apres transaction 3.");
        console.value("UPDATE Hibernate envoyes", diagnostics.entityUpdateCount());
        console.check("Aucun UPDATE envoye par une projection DTO", diagnostics.entityUpdateCount() == 0);
        console.check("Projection encore disponible apres transaction", !sales.isEmpty());
    }
}
