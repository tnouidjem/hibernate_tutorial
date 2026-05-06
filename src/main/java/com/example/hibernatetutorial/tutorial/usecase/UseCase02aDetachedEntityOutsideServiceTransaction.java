package com.example.hibernatetutorial.tutorial.usecase;

import com.example.hibernatetutorial.domain.Product;
import com.example.hibernatetutorial.repository.ProductRepository;
import com.example.hibernatetutorial.tutorial.HibernateDiagnostics;
import com.example.hibernatetutorial.tutorial.TutorialConsole;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.example.hibernatetutorial.tutorial.usecase.UseCaseProductCodes.HEADSET_PRODUCT_CODE;

@Component
public class UseCase02aDetachedEntityOutsideServiceTransaction implements HibernateUseCase {

    private final ProductRepository productRepository;
    private final TutorialConsole console;
    private final HibernateDiagnostics diagnostics;

    public UseCase02aDetachedEntityOutsideServiceTransaction(
            ProductRepository productRepository,
            TutorialConsole console,
            HibernateDiagnostics diagnostics
    ) {
        this.productRepository = productRepository;
        this.console = console;
        this.diagnostics = diagnostics;
    }

    /**
     * But de la demonstration: montrer qu'une entite modifiee hors transaction de service n'est pas persistee.
     * Explication technique: l'appel repository ouvre puis ferme sa propre transaction. L'entite retournee devient
     * detachee; un setter change seulement l'objet Java en memoire, sans Persistence Context actif pour flusher.
     */
    @Override
    public void run() {
        console.title("2a. Sans transaction de service: modification d'une entite detachee");
        diagnostics.print("debut");

        // ETAPE 2a.1 - Charger le produit via le repository sans transaction de service englobante.
        Product headset = productRepository.findByProductCode(HEADSET_PRODUCT_CODE).orElseThrow();
        diagnostics.print("apres appel repository");
        BigDecimal originalPrice = headset.getPrice();

        // ETAPE 2a.2 - Modifier l'entite detachee en memoire.
        headset.setPrice(new BigDecimal("1.00"));

        // ETAPE 2a.3 - Relire le produit depuis la base pour comparer avec l'objet modifie en memoire.
        Product reloaded = productRepository.findByProductCode(HEADSET_PRODUCT_CODE).orElseThrow();

        // ETAPE 2a.4 - Afficher la preuve que la modification locale n'a pas ete persistee.
        console.step("Le changement local n'est pas persiste, car aucun Persistence Context n'est actif autour du use case.");
        console.value("Prix modifie en memoire", headset.getPrice());
        console.value("Prix relu en base", reloaded.getPrice());
        console.value("Prix initial conserve", originalPrice.equals(reloaded.getPrice()));
        diagnostics.print("fin");
    }
}
