package com.example.hibernatetutorial;

import com.example.hibernatetutorial.bootstrap.DataSeeder;
import com.example.hibernatetutorial.tutorial.HibernateAdvancedUseCases;
import com.example.hibernatetutorial.tutorial.usecase.HibernateUseCase;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Comparator;
import java.util.List;

@SpringBootApplication
public class HibernateTutorialApplication implements CommandLineRunner {

    private final DataSeeder dataSeeder;
    private final List<HibernateUseCase> hibernateUseCases;
    private final HibernateAdvancedUseCases hibernateAdvancedUseCases;

    public HibernateTutorialApplication(
            DataSeeder dataSeeder,
            List<HibernateUseCase> hibernateUseCases,
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

        hibernateUseCases.stream()
                .sorted(Comparator.comparing(useCase -> AopUtils.getTargetClass(useCase).getSimpleName()))
                .forEach(HibernateUseCase::run);

        hibernateAdvancedUseCases.demonstrateIsolationLevels();
        hibernateAdvancedUseCases.demonstrateFirstLevelCacheCanHideIsolationEffects();
        hibernateAdvancedUseCases.demonstrateRequiredPropagationRollback();
        hibernateAdvancedUseCases.demonstrateRequiresNewPropagation();
    }
}
