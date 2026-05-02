# Tutorial Hibernate avec Spring Boot 4

Projet console, sans API REST, concu pour presenter Hibernate/JPA a des developpeurs juniors.

## Lancer le projet

```powershell
mvn spring-boot:run
```

Le projet demarre depuis `HibernateTutorialApplication.main`, charge une base H2 en memoire avec des donnees fictives de site marchand, puis execute plusieurs demonstrations dans la console.

## Scenarios couverts

0. **Repository par id et cache de premier niveau**
   - Deux appels successifs a `productRepository.findById(...)` dans la meme transaction ne declenchent pas deux SELECT.
   - `findById(...)` passe par l'identifiant primaire, que le cache de premier niveau sait utiliser directement.

0b. **Repository par critere et cache de premier niveau**
   - Deux appels successifs a `productRepository.findBySku(...)` executent deux SELECT, car le cache de premier niveau est indexe par id et non par `sku`.
   - Un `setName(...)` sur la premiere variable est immediatement visible depuis la seconde.
   - Comme la transaction commit, Hibernate detecte le changement et execute un `UPDATE` sans appel a `save(...)`.

1. **Cache de premier niveau**
   - Deux `find` du meme `Product` dans la meme transaction retournent la meme instance Java.
   - Un `setPrice(...)` sur la premiere variable est immediatement visible depuis la seconde variable.
   - Apres `EntityManager.clear()`, Hibernate doit relire l'entite.

2. **Impact des transactions**
   - Une entite modifiee hors transaction de service devient detachee: le changement n'est pas persiste.
   - Dans une transaction read-only, un setter puis un `save(...)` explicite ne provoquent pas d'`UPDATE`.
   - Un appel interne a une methode `@Transactional` de la meme classe ne declenche pas de transaction Spring, car l'appel ne passe pas par le proxy.
   - Une relation LAZY accedee hors transaction declenche une `LazyInitializationException`.

3. **DTO projections et transactions read-only**
   - Une requete JPQL construit directement des `ProductSalesDto`.
   - La transaction `readOnly = true` est adaptee aux lectures et evite de gerer inutilement des entites quand un DTO suffit.

4. **Impact du flush**
   - En mode `AUTO`, Hibernate flush les changements avant certaines requetes JPQL pour garder une lecture coherente.
   - En mode `COMMIT`, Hibernate peut reporter le flush jusqu'a la fin de transaction: une requete peut donc ne pas voir une modification en attente.

5. **Isolation**
   - Comparaison entre `READ_COMMITTED` et `REPEATABLE_READ` avec deux transactions successives.
   - Exemple montrant que le cache de premier niveau peut masquer un commit externe si une entite est deja managee.

6. **Propagation**
   - `REQUIRED`: l'appel interne participe a la transaction externe; un rollback interne marque toute la transaction.
   - `REQUIRES_NEW`: l'appel interne ouvre une transaction separee qui peut commit meme si la transaction externe rollback.

## Fichiers utiles pour la presentation

- `src/main/java/com/example/hibernatetutorial/HibernateTutorialApplication.java`: point d'entree.
- `src/main/java/com/example/hibernatetutorial/tutorial/HibernateUseCases.java`: demonstrations commentees.
- `src/main/java/com/example/hibernatetutorial/tutorial/HibernateAdvancedUseCases.java`: demonstrations avancees isolation/propagation.
- `src/main/java/com/example/hibernatetutorial/bootstrap/DataSeeder.java`: jeu de donnees marchand.
- `src/main/resources/application.properties`: configuration H2, JPA et logs SQL Hibernate.

## Notes pedagogiques

Les logs SQL sont volontairement actives. Pendant la presentation, demandez aux juniors de comparer:

- ce que le code Java semble faire;
- le nombre de requetes SQL reellement executees;
- le moment ou les `UPDATE` partent en base;
- la difference entre une entite managee, detachee et un simple DTO.
