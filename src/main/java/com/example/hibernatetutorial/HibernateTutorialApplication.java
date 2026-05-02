package com.example.hibernatetutorial;

import com.example.hibernatetutorial.bootstrap.DataSeeder;
import com.example.hibernatetutorial.tutorial.HibernateAdvancedUseCases;
import com.example.hibernatetutorial.tutorial.HibernateUseCases;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HibernateTutorialApplication implements CommandLineRunner {

    private final DataSeeder dataSeeder;
    private final HibernateUseCases hibernateUseCases;
    private final HibernateAdvancedUseCases hibernateAdvancedUseCases;

    public HibernateTutorialApplication(
            DataSeeder dataSeeder,
            HibernateUseCases hibernateUseCases,
            HibernateAdvancedUseCases hibernateAdvancedUseCases
    ) {
        this.dataSeeder = dataSeeder;
        this.hibernateUseCases = hibernateUseCases;
        this.hibernateAdvancedUseCases = hibernateAdvancedUseCases;
    }

    public static void main(String[] args) {
        SpringApplication.run(HibernateTutorialApplication.class, args);
    }

    @Override
    public void run(String... args) {
        dataSeeder.resetAndLoad();

        hibernateUseCases.demonstrateRepositoryFindByIdUsesFirstLevelCache();
        hibernateUseCases.demonstrateRepositoryCallsShareFirstLevelCache();
        hibernateUseCases.demonstrateFirstLevelCache();

        hibernateUseCases.demonstrateDetachedEntityOutsideServiceTransaction();
        hibernateUseCases.demonstrateDirtyCheckingInsideTransaction();
        hibernateUseCases.verifyReadOnlySaveWasNotCommitted();
        hibernateUseCases.demonstrateTransactionalSelfInvocationHasNoEffect();
        hibernateUseCases.demonstrateLazyLoadingOutsideTransaction();
        hibernateUseCases.demonstrateLazyLoadingInsideTransaction();

        hibernateUseCases.demonstrateDtoProjectionInReadOnlyTransaction();

        hibernateUseCases.demonstrateAutoFlushBeforeQuery();
        hibernateUseCases.demonstrateCommitFlushModeBeforeQuery();

        hibernateAdvancedUseCases.demonstrateIsolationLevels();
        hibernateAdvancedUseCases.demonstrateFirstLevelCacheCanHideIsolationEffects();
        hibernateAdvancedUseCases.demonstrateRequiredPropagationRollback();
        hibernateAdvancedUseCases.demonstrateRequiresNewPropagation();
    }
}
