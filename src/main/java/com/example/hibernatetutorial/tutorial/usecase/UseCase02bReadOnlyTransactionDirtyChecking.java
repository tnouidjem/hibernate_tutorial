package com.example.hibernatetutorial.tutorial.usecase;

import com.example.hibernatetutorial.domain.Product;
import com.example.hibernatetutorial.repository.ProductRepository;
import com.example.hibernatetutorial.tutorial.HibernateDiagnostics;
import com.example.hibernatetutorial.tutorial.TutorialConsole;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

import static com.example.hibernatetutorial.tutorial.usecase.UseCaseProductCodes.KEYBOARD_INITIAL_PRICE;
import static com.example.hibernatetutorial.tutorial.usecase.UseCaseProductCodes.KEYBOARD_PRODUCT_CODE;

@Component
public class UseCase02bReadOnlyTransactionDirtyChecking implements HibernateUseCase {

    private final EntityManager entityManager;
    private final ProductRepository productRepository;
    private final TutorialConsole console;
    private final HibernateDiagnostics diagnostics;

    public UseCase02bReadOnlyTransactionDirtyChecking(
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
     * But de la demonstration: montrer qu'une transaction read-only n'est pas faite pour modifier des donnees.
     * Explication technique: Spring configure Hibernate en mode lecture, notamment avec un flush manual. Meme si
     * l'entite est modifiee en memoire et meme si save(...) est appele explicitement, Hibernate ne flush pas ce
     * changement au commit de cette transaction read-only.
     */
    @Override
    @Transactional(readOnly = true)
    public void run() {
        console.title("2b. Transaction read-only: setter + save explicite ne modifient pas la base");
        diagnostics.resetStatistics();
        diagnostics.print("debut");

        // ETAPE 2b.1 - Recuperer la session Hibernate pour exposer l'etat read-only.
        Session session = entityManager.unwrap(Session.class);

        // ETAPE 2b.2 - Charger le clavier et memoriser son prix initial.
        Product keyboard = productRepository.findByProductCode(KEYBOARD_PRODUCT_CODE).orElseThrow();
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

    @Override
    public void after() {
        Product keyboard = productRepository.findByProductCode(KEYBOARD_PRODUCT_CODE).orElseThrow();

        console.step("Verification apres transaction 2b.");
        console.value("UPDATE Hibernate envoyes", diagnostics.entityUpdateCount());
        console.check("Aucun UPDATE envoye en read-only", diagnostics.entityUpdateCount() == 0);
        console.value("Prix relu en base", keyboard.getPrice());
        console.check("Transaction read-only sans effet durable", KEYBOARD_INITIAL_PRICE.compareTo(keyboard.getPrice()) == 0);
    }
}
