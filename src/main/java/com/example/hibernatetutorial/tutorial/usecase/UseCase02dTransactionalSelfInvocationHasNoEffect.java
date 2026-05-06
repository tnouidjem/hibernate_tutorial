package com.example.hibernatetutorial.tutorial.usecase;

import com.example.hibernatetutorial.domain.Product;
import com.example.hibernatetutorial.repository.ProductRepository;
import com.example.hibernatetutorial.tutorial.HibernateDiagnostics;
import com.example.hibernatetutorial.tutorial.TutorialConsole;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

import static com.example.hibernatetutorial.tutorial.usecase.UseCaseSkus.HEADSET_SKU;

@Component
public class UseCase02dTransactionalSelfInvocationHasNoEffect implements HibernateUseCase {

    private final ProductRepository productRepository;
    private final TutorialConsole console;
    private final HibernateDiagnostics diagnostics;

    public UseCase02dTransactionalSelfInvocationHasNoEffect(
            ProductRepository productRepository,
            TutorialConsole console,
            HibernateDiagnostics diagnostics
    ) {
        this.productRepository = productRepository;
        this.console = console;
        this.diagnostics = diagnostics;
    }

    /**
     * But de la demonstration: montrer le piege de la self-invocation avec @Transactional.
     * Explication technique: Spring applique @Transactional via un proxy. Un appel a une methode de la meme classe
     * est un appel Java direct, donc il ne traverse pas le proxy et l'annotation transactionnelle n'est pas appliquee.
     */
    @Override
    public void run() {
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
}
