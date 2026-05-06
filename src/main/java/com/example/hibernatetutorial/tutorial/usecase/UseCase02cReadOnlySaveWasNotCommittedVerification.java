package com.example.hibernatetutorial.tutorial.usecase;

import com.example.hibernatetutorial.domain.Product;
import com.example.hibernatetutorial.repository.ProductRepository;
import com.example.hibernatetutorial.tutorial.HibernateDiagnostics;
import com.example.hibernatetutorial.tutorial.TutorialConsole;
import org.springframework.stereotype.Component;

import static com.example.hibernatetutorial.tutorial.usecase.UseCaseProductCodes.KEYBOARD_PRODUCT_CODE;

@Component
public class UseCase02cReadOnlySaveWasNotCommittedVerification implements HibernateUseCase {

    private final ProductRepository productRepository;
    private final TutorialConsole console;
    private final HibernateDiagnostics diagnostics;

    public UseCase02cReadOnlySaveWasNotCommittedVerification(
            ProductRepository productRepository,
            TutorialConsole console,
            HibernateDiagnostics diagnostics
    ) {
        this.productRepository = productRepository;
        this.console = console;
        this.diagnostics = diagnostics;
    }

    /**
     * But de la demonstration: verifier que le setter + save(...) en read-only n'ont pas ete commites.
     * Explication technique: cette lecture se fait apres la transaction read-only precedente. Si aucun UPDATE n'a
     * ete flush, la base contient encore le prix initial du clavier.
     */
    @Override
    public void run() {
        console.title("2b-verification. Verification apres transaction read-only");
        diagnostics.print("debut");

        // ETAPE 2b-verification.1 - Relire le clavier apres la transaction read-only precedente.
        Product keyboard = productRepository.findByProductCode(KEYBOARD_PRODUCT_CODE).orElseThrow();

        // ETAPE 2b-verification.2 - Afficher le prix effectivement conserve en base.
        console.value("Prix relu apres transaction read-only", keyboard.getPrice());
        diagnostics.print("fin");
    }
}
