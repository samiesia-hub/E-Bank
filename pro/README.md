# E-Banking Application

Application web de gestion bancaire développée avec Spring Boot et REACT. Le projet permet de mettre en pratique le développement full-stack, la gestion d'une base de données relationnelle et le déploiement avec Docker.

## Technologies utilisées

### Backend
- Java
- Spring Boot
- Maven
- Spring Data JPA
- Spring Security



### DevOps
- Git et GitHub
- Docker
- Docker Compose
- Kubernetes 
- Jenkins


## Architecture du projet


Adaptez cette structure aux dossiers présents dans le dépôt.

## Prérequis

Pour exécuter le projet en développement :

- JDK compatible avec le `pom.xml`
- IntelliJ IDEA
- Maven

Pour le lancement conteneurisé :

- Docker
- Docker Compose

## 1. Cloner le dépôt


## 2. Lancer le backend avec IntelliJ IDEA

1. Ouvrez IntelliJ IDEA.
2. Cliquez sur **Open**.
3. Sélectionnez le dossier `backend`.
4. Importez le projet Maven à partir du fichier `pom.xml`.
5. Configurez le JDK compatible avec le projet.
6. Attendez que Maven télécharge les dépendances.
7. Ouvrez la classe principale de Spring Boot.
8. Cliquez sur **Run** pour démarrer l'application.

script n'existe pas, vérifiez les scripts disponibles dans `package.json`. Une application Angular utilise souvent `ng serve` pour le développement.




## 7. Commandes Maven utiles

Compiler le projet :

```bash
mvn clean compile
```

Exécuter les tests :

```bash
mvn test
```

Construire le package :

```bash
mvn clean package
```

Lancer Spring Boot :

```bash
mvn spring-boot:run
```

## 8. Base de données (Docker)

Le plus simple : PostgreSQL dans Docker, sans rien installer.

```bash
docker compose up -d      # démarre PostgreSQL (base ebank / utilisateur ebank / mot de passe ebank)
docker compose ps         # la colonne STATUS doit afficher (healthy)
```

Puis lancez le backend :

```bash
cd pro
./mvnw spring-boot:run
```

Par défaut le backend se connecte à `jdbc:postgresql://localhost:5432/ebank` avec `ebank` / `ebank`,
c'est-à-dire les valeurs de `docker-compose.yml`. Pour une autre base, définissez `DB_URL`, `DB_USER` et
`DB_PASSWORD`. Les tests, eux, utilisent H2 en mémoire et ne demandent aucun conteneur.

```bash
docker compose stop         # arrête la base (les données restent dans le volume)
docker compose down -v      # supprime aussi les données
```

Authentification : le frontend (`e-bank-frontend`) et le backend doivent partager le **même** `JWT_SECRET`
(32 caractères minimum). Sans `JWT_SECRET`, le backend génère une clé aléatoire à chaque démarrage et les
cookies du frontend ne sont plus valides.

## 9. Dépannage


### Problème de version Java
- Vérifiez `java -version`.
- Vérifiez la version utilisée par Maven avec `mvn -version`.
- Vérifiez le JDK configuré dans IntelliJ IDEA.
- Respectez la version Java déclarée dans le `pom.xml`.


## Auteur

Projet académique Taoufik Mansouri
