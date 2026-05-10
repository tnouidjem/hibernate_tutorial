package com.example.hibernatetutorial.tutorial.usecase;

import com.example.hibernatetutorial.domain.Product;
import com.example.hibernatetutorial.repository.ProductRepository;
import com.example.hibernatetutorial.tutorial.HibernateDiagnostics;
import com.example.hibernatetutorial.tutorial.TutorialConsole;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.example.hibernatetutorial.tutorial.usecase.UseCaseProductCodes.LAPTOP_PRODUCT_CODE;
import static com.example.hibernatetutorial.tutorial.usecase.UseCaseProductCodes.LAPTOP_NAME_AFTER_FIND_BY_ID_DEMO;

@Component
public class UseCase00aRepositoryFindByIdUsesFirstLevelCache implements HibernateUseCase {

    private final ProductRepository productRepository;
    private final TutorialConsole console;
    private final HibernateDiagnostics diagnostics;

    public UseCase00aRepositoryFindByIdUsesFirstLevelCache(
            ProductRepository productRepository,
            TutorialConsole console,
            HibernateDiagnostics diagnostics
    ) {
        this.productRepository = productRepository;
        this.console = console;
        this.diagnostics = diagnostics;
    }

    /**
     * But de la demonstration: montrer le cas le plus direct ou le cache de premier niveau evite un deuxieme SELECT.
     * Explication technique: findById(...) delegue a EntityManager.find(...), qui consulte d'abord le Persistence
     * Context par cle primaire. Une fois l'entite chargee, le deuxieme findById(...) retourne l'instance managee
     * sans requete SQL supplementaire.
     */
    @Override
    @Transactional
    public void run() {
        console.title("0a. Repository findById: le deuxieme acces par id ne refait pas de SELECT");
        diagnostics.resetStatistics();
        diagnostics.print("debut");

        // ETAPE 0a.1 - Recuperer l'identifiant qui servira aux deux lectures par cle primaire.
        Long productId = productRepository.findIdByProductCode(LAPTOP_PRODUCT_CODE);

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

    @Override
    public void after() {
        Product laptop = productRepository.findByProductCode(LAPTOP_PRODUCT_CODE).orElseThrow();

        console.step("Verification apres transaction 0a.");
        console.value("UPDATE Hibernate envoyes", diagnostics.entityUpdateCount());
        console.check("Un UPDATE a ete envoye", diagnostics.entityUpdateCount() == 1);
        console.value("Nom relu en base", laptop.getName());
        console.check("Transaction validee", LAPTOP_NAME_AFTER_FIND_BY_ID_DEMO.equals(laptop.getName()));
    }
}
