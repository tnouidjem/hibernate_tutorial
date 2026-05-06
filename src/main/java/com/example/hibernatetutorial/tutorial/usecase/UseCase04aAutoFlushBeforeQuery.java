package com.example.hibernatetutorial.tutorial.usecase;

import com.example.hibernatetutorial.domain.Product;
import com.example.hibernatetutorial.repository.ProductRepository;
import com.example.hibernatetutorial.tutorial.HibernateDiagnostics;
import com.example.hibernatetutorial.tutorial.TutorialConsole;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.util.List;

import static com.example.hibernatetutorial.tutorial.usecase.UseCaseSkus.LAPTOP_SKU;

@Component
public class UseCase04aAutoFlushBeforeQuery implements HibernateUseCase {

    private final ProductRepository productRepository;
    private final TutorialConsole console;
    private final HibernateDiagnostics diagnostics;

    public UseCase04aAutoFlushBeforeQuery(
            ProductRepository productRepository,
            TutorialConsole console,
            HibernateDiagnostics diagnostics
    ) {
        this.productRepository = productRepository;
        this.console = console;
        this.diagnostics = diagnostics;
    }

    /**
     * But de la demonstration: montrer que le flush AUTO peut declencher un UPDATE avant un SELECT.
     * Explication technique: avant certaines requetes JPQL, Hibernate synchronise les changements en attente pour
     * que la requete lise un etat coherent avec le Persistence Context. Le SELECT voit donc la modification locale.
     */
    @Override
    @Transactional
    public void run() {
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
}
