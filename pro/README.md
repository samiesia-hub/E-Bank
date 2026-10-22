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

## 8. Dépannage


### Problème de version Java
- Vérifiez `java -version`.
- Vérifiez la version utilisée par Maven avec `mvn -version`.
- Vérifiez le JDK configuré dans IntelliJ IDEA.
- Respectez la version Java déclarée dans le `pom.xml`.


## Auteur

Projet académique Taoufik Mansouri
