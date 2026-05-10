package com.example.hibernatetutorial;

import com.example.hibernatetutorial.bootstrap.DataSeeder;
import com.example.hibernatetutorial.tutorial.HibernateAdvancedUseCases;
import com.example.hibernatetutorial.tutorial.usecase.UseCase00aRepositoryFindByIdUsesFirstLevelCache;
import com.example.hibernatetutorial.tutorial.usecase.UseCase00bRepositoryCallsShareFirstLevelCache;
import com.example.hibernatetutorial.tutorial.usecase.UseCase01FirstLevelCache;
import com.example.hibernatetutorial.tutorial.usecase.UseCase02aDetachedEntityOutsideServiceTransaction;
import com.example.hibernatetutorial.tutorial.usecase.UseCase02bReadOnlyTransactionDirtyChecking;
import com.example.hibernatetutorial.tutorial.usecase.UseCase02cReadOnlySaveWasNotCommittedVerification;
import com.example.hibernatetutorial.tutorial.usecase.UseCase02dTransactionalSelfInvocationHasNoEffect;
import com.example.hibernatetutorial.tutorial.usecase.UseCase02eLazyLoadingOutsideTransaction;
import com.example.hibernatetutorial.tutorial.usecase.UseCase02fLazyLoadingInsideTransaction;
import com.example.hibernatetutorial.tutorial.usecase.UseCase03DtoProjectionInReadOnlyTransaction;
import com.example.hibernatetutorial.tutorial.usecase.UseCase04aAutoFlushBeforeQuery;
import com.example.hibernatetutorial.tutorial.usecase.UseCase04bCommitFlushModeBeforeQuery;
import com.example.hibernatetutorial.tutorial.usecase.UseCase04cPersistenceContextGrowthInLoop;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HibernateTutorialApplication implements CommandLineRunner {

    private final DataSeeder dataSeeder;
    private final UseCase00aRepositoryFindByIdUsesFirstLevelCache useCase00aRepositoryFindByIdUsesFirstLevelCache;
    private final UseCase00bRepositoryCallsShareFirstLevelCache useCase00bRepositoryCallsShareFirstLevelCache;
    private final UseCase01FirstLevelCache useCase01FirstLevelCache;
    private final UseCase02aDetachedEntityOutsideServiceTransaction useCase02aDetachedEntityOutsideServiceTransaction;
    private final UseCase02bReadOnlyTransactionDirtyChecking useCase02bReadOnlyTransactionDirtyChecking;
    private final UseCase02cReadOnlySaveWasNotCommittedVerification useCase02cReadOnlySaveWasNotCommittedVerification;
    private final UseCase02dTransactionalSelfInvocationHasNoEffect useCase02dTransactionalSelfInvocationHasNoEffect;
    private final UseCase02eLazyLoadingOutsideTransaction useCase02eLazyLoadingOutsideTransaction;
    private final UseCase02fLazyLoadingInsideTransaction useCase02fLazyLoadingInsideTransaction;
    private final UseCase03DtoProjectionInReadOnlyTransaction useCase03DtoProjectionInReadOnlyTransaction;
    private final UseCase04aAutoFlushBeforeQuery useCase04aAutoFlushBeforeQuery;
    private final UseCase04bCommitFlushModeBeforeQuery useCase04bCommitFlushModeBeforeQuery;
    private final UseCase04cPersistenceContextGrowthInLoop useCase04cPersistenceContextGrowthInLoop;
    private final HibernateAdvancedUseCases hibernateAdvancedUseCases;

    public HibernateTutorialApplication(
            DataSeeder dataSeeder,
            UseCase00aRepositoryFindByIdUsesFirstLevelCache useCase00aRepositoryFindByIdUsesFirstLevelCache,
            UseCase00bRepositoryCallsShareFirstLevelCache useCase00bRepositoryCallsShareFirstLevelCache,
            UseCase01FirstLevelCache useCase01FirstLevelCache,
            UseCase02aDetachedEntityOutsideServiceTransaction useCase02aDetachedEntityOutsideServiceTransaction,
            UseCase02bReadOnlyTransactionDirtyChecking useCase02bReadOnlyTransactionDirtyChecking,
            UseCase02cReadOnlySaveWasNotCommittedVerification useCase02cReadOnlySaveWasNotCommittedVerification,
            UseCase02dTransactionalSelfInvocationHasNoEffect useCase02dTransactionalSelfInvocationHasNoEffect,
            UseCase02eLazyLoadingOutsideTransaction useCase02eLazyLoadingOutsideTransaction,
            UseCase02fLazyLoadingInsideTransaction useCase02fLazyLoadingInsideTransaction,
            UseCase03DtoProjectionInReadOnlyTransaction useCase03DtoProjectionInReadOnlyTransaction,
            UseCase04aAutoFlushBeforeQuery useCase04aAutoFlushBeforeQuery,
            UseCase04bCommitFlushModeBeforeQuery useCase04bCommitFlushModeBeforeQuery,
            UseCase04cPersistenceContextGrowthInLoop useCase04cPersistenceContextGrowthInLoop,
            HibernateAdvancedUseCases hibernateAdvancedUseCases
    ) {
        this.dataSeeder = dataSeeder;
        this.useCase00aRepositoryFindByIdUsesFirstLevelCache = useCase00aRepositoryFindByIdUsesFirstLevelCache;
        this.useCase00bRepositoryCallsShareFirstLevelCache = useCase00bRepositoryCallsShareFirstLevelCache;
        this.useCase01FirstLevelCache = useCase01FirstLevelCache;
        this.useCase02aDetachedEntityOutsideServiceTransaction = useCase02aDetachedEntityOutsideServiceTransaction;
        this.useCase02bReadOnlyTransactionDirtyChecking = useCase02bReadOnlyTransactionDirtyChecking;
        this.useCase02cReadOnlySaveWasNotCommittedVerification = useCase02cReadOnlySaveWasNotCommittedVerification;
        this.useCase02dTransactionalSelfInvocationHasNoEffect = useCase02dTransactionalSelfInvocationHasNoEffect;
        this.useCase02eLazyLoadingOutsideTransaction = useCase02eLazyLoadingOutsideTransaction;
        this.useCase02fLazyLoadingInsideTransaction = useCase02fLazyLoadingInsideTransaction;
        this.useCase03DtoProjectionInReadOnlyTransaction = useCase03DtoProjectionInReadOnlyTransaction;
        this.useCase04aAutoFlushBeforeQuery = useCase04aAutoFlushBeforeQuery;
        this.useCase04bCommitFlushModeBeforeQuery = useCase04bCommitFlushModeBeforeQuery;
        this.useCase04cPersistenceContextGrowthInLoop = useCase04cPersistenceContextGrowthInLoop;
        this.hibernateAdvancedUseCases = hibernateAdvancedUseCases;
    }

    public static void main(String[] args) {
        SpringApplication.run(HibernateTutorialApplication.class, args);
    }

    @Override
    public void run(String... args) {
        dataSeeder.resetAndLoad();

        // Teste que findById reutilise le cache de premier niveau pour un deuxieme acces par id.
        useCase00aRepositoryFindByIdUsesFirstLevelCache.run();
        useCase00aRepositoryFindByIdUsesFirstLevelCache.after();

        // Teste que deux appels repository par critere relancent le SELECT mais partagent l'instance managee.
        useCase00bRepositoryCallsShareFirstLevelCache.run();
        useCase00bRepositoryCallsShareFirstLevelCache.after();

        // Teste l'identite Java garantie par le Persistence Context et l'effet d'un clear().
        useCase01FirstLevelCache.run();
        useCase01FirstLevelCache.after();

        // Teste qu'une entite modifiee hors transaction de service reste detachee et n'est pas persistee.
        useCase02aDetachedEntityOutsideServiceTransaction.run();
        useCase02aDetachedEntityOutsideServiceTransaction.after();

        // Teste l'effet d'une transaction read-only sur le dirty checking et un save explicite.
        useCase02bReadOnlyTransactionDirtyChecking.run();
        useCase02bReadOnlyTransactionDirtyChecking.after();

        // Verifie que les modifications tentees dans la transaction read-only n'ont pas ete validees.
        useCase02cReadOnlySaveWasNotCommittedVerification.run();
        useCase02cReadOnlySaveWasNotCommittedVerification.after();

        // Teste le piege de la self-invocation: @Transactional n'est pas applique sans passage par le proxy.
        useCase02dTransactionalSelfInvocationHasNoEffect.run();
        useCase02dTransactionalSelfInvocationHasNoEffect.after();

        // Teste l'echec du LAZY loading quand la relation est accedee hors transaction.
        useCase02eLazyLoadingOutsideTransaction.run();
        useCase02eLazyLoadingOutsideTransaction.after();

        // Teste que le LAZY loading fonctionne tant que la transaction reste ouverte.
        useCase02fLazyLoadingInsideTransaction.run();
        useCase02fLazyLoadingInsideTransaction.after();

        // Teste les projections DTO pour lire sans charger tout le graphe d'entites.
        useCase03DtoProjectionInReadOnlyTransaction.run();
        useCase03DtoProjectionInReadOnlyTransaction.after();

        // Teste le flush AUTO: une requete JPQL peut declencher un UPDATE avant son SELECT.
        useCase04aAutoFlushBeforeQuery.run();
        useCase04aAutoFlushBeforeQuery.after();

        // Teste le mode FlushModeType.COMMIT et le report possible de l'UPDATE.
        useCase04bCommitFlushModeBeforeQuery.run();
        useCase04bCommitFlushModeBeforeQuery.after();

        // Teste la croissance du Persistence Context pendant une boucle de modifications.
        useCase04cPersistenceContextGrowthInLoop.run();
        useCase04cPersistenceContextGrowthInLoop.after();

        // Teste les niveaux d'isolation avec deux transactions concurrentes.
        hibernateAdvancedUseCases.demonstrateIsolationLevels();
        hibernateAdvancedUseCases.afterDemonstrateIsolationLevels();
        // Teste comment le cache de premier niveau peut masquer un commit externe.
        hibernateAdvancedUseCases.demonstrateFirstLevelCacheCanHideIsolationEffects();
        hibernateAdvancedUseCases.afterDemonstrateFirstLevelCacheCanHideIsolationEffects();
        // Teste qu'un rollback interne en REQUIRED marque toute la transaction rollback-only.
        hibernateAdvancedUseCases.demonstrateRequiredPropagationRollback();
        hibernateAdvancedUseCases.afterDemonstrateRequiredPropagationRollback();
        // Teste qu'un appel REQUIRES_NEW commit independamment de la transaction appelante.
        hibernateAdvancedUseCases.demonstrateRequiresNewPropagation();
        hibernateAdvancedUseCases.afterDemonstrateRequiresNewPropagation();
    }
}
