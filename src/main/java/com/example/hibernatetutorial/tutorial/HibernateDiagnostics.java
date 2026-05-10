package com.example.hibernatetutorial.tutorial;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.stat.Statistics;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class HibernateDiagnostics {

    private final EntityManager entityManager;
    private final EntityManagerFactory entityManagerFactory;
    private final HikariDataSource dataSource;
    private final TutorialConsole console;

    public HibernateDiagnostics(
            EntityManager entityManager,
            EntityManagerFactory entityManagerFactory,
            HikariDataSource dataSource,
            TutorialConsole console
    ) {
        this.entityManager = entityManager;
        this.entityManagerFactory = entityManagerFactory;
        this.dataSource = dataSource;
        this.console = console;
    }

    public void print(String label) {
        console.step("Diagnostics - " + label);
        console.value("Transaction active", TransactionSynchronizationManager.isActualTransactionActive());
        console.value("Entites managees", managedEntityCount());
        printConnectionPoolState();
    }

    public void resetStatistics() {
        statistics().clear();
    }

    public long entityUpdateCount() {
        return statistics().getEntityUpdateCount();
    }

    private Object managedEntityCount() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return "n/a hors transaction";
        }

        try {
            SessionImplementor session = entityManager.unwrap(SessionImplementor.class);
            return session.getPersistenceContext().getNumberOfManagedEntities();
        } catch (RuntimeException ex) {
            return "indisponible: " + ex.getClass().getSimpleName();
        }
    }

    private void printConnectionPoolState() {
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        if (pool == null) {
            console.value("Pool connexions", "indisponible");
            return;
        }

        console.value("Connexions actives", pool.getActiveConnections());
        console.value("Connexions idle", pool.getIdleConnections());
        console.value("Connexions totales", pool.getTotalConnections());
        console.value("Threads attente connexion", pool.getThreadsAwaitingConnection());
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }
}
