package com.example.hibernatetutorial.tutorial.usecase;

import com.example.hibernatetutorial.domain.Product;
import com.example.hibernatetutorial.repository.ProductRepository;
import com.example.hibernatetutorial.tutorial.HibernateDiagnostics;
import com.example.hibernatetutorial.tutorial.TutorialConsole;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.example.hibernatetutorial.tutorial.usecase.UseCaseProductCodes.LAPTOP_PRODUCT_CODE;

@Component
public class UseCase00bRepositoryCallsShareFirstLevelCache implements HibernateUseCase {

    private final ProductRepository productRepository;
    private final TutorialConsole console;
    private final HibernateDiagnostics diagnostics;

    public UseCase00bRepositoryCallsShareFirstLevelCache(
            ProductRepository productRepository,
            TutorialConsole console,
            HibernateDiagnostics diagnostics
    ) {
        this.productRepository = productRepository;
        this.console = console;
        this.diagnostics = diagnostics;
    }

    /**
     * But de la demonstration: montrer que deux appels repository successifs peuvent retourner la meme instance Java.
     * Explication technique: dans une transaction, Spring Data JPA utilise l'EntityManager lie a la transaction.
     * Une requete par critere comme findByProductCode(...) execute toutefois le SELECT a chaque appel, car le cache de
     * premier niveau est indexe par id. Hibernate reutilise ensuite l'instance deja managee pour cet id.
     */
    @Override
    @Transactional
    public void run() {
        console.title("0b. Repository findByProductCode: deux SELECT, mais une seule instance Java");
        diagnostics.print("debut");

        // ETAPE 0b.1 - Executer un premier appel Spring Data JPA par critere.
        Product firstRepositoryLoad = productRepository.findByProductCode(LAPTOP_PRODUCT_CODE).orElseThrow();
        diagnostics.print("apres premier findByProductCode");

        // ETAPE 0b.2 - Executer le meme appel: le SELECT repart, mais Hibernate reutilise l'instance managee.
        Product secondRepositoryLoad = productRepository.findByProductCode(LAPTOP_PRODUCT_CODE).orElseThrow();
        diagnostics.print("apres second findByProductCode");

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
}
