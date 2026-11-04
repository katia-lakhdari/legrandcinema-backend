# Le Grand Cinéma (backend)

C'est le backend de mon projet de fin de Bachelor Développeur Java chez STUDI (Bloc 3).
Le Grand Cinéma est un cinéma qui veut que ses spectateurs puissent réserver leurs places en ligne au lieu de faire la queue. J'ai fait le projet seule.

Le frontend est dans un autre dépôt : [legrandcinema-frontend](https://github.com/katia-lakhdari/legrandcinema-frontend).

## Ce que fait l'API

Sans compte, on peut voir les films à l'affiche et leurs séances.

Pour réserver il faut un compte client. Le client choisit ses places, elles sont bloquées pour lui pendant 5 minutes, le temps de payer. S'il ne paie pas, elles se libèrent toutes seules. Une fois le paiement fait, il reçoit un e-mail avec son billet et le QR code, et il peut retrouver ses billets dans "Mes billets". Il y a aussi le mot de passe oublié : on reçoit un lien par e-mail, qui marche 30 minutes.

L'admin s'occupe des films, des salles et des séances. Il peut bloquer des places, scanner les billets à l'entrée (un billet déjà scanné est refusé) et voir comment se remplissent les séances.

## Stack

- Java 21, Spring Boot 4.1, Maven
- MySQL 8 avec Spring Data JPA
- Spring Security et JWT
- Stripe (mode test) pour le paiement
- Brevo pour les e-mails
- ZXing pour les QR codes
- Swagger pour la doc de l'API
- JUnit 5, Mockito et H2 pour les tests, JaCoCo pour la couverture
- SonarQube Cloud
- Docker, Render et Aiven pour la mise en ligne

## Architecture

Le code est rangé par couches dans `com.legrandcinema`.

Les `controller` reçoivent les requêtes, les `service` font le travail (c'est là qu'il y a les règles, par exemple le verrouillage des places ou le paiement) et les `repository` parlent à la base. Les tables sont dans `entity` : Film, Salle, Seance, Place, Reservation, Billet et Utilisateur.

Je n'envoie jamais les entités directement, je passe par des DTO (`dto/request` pour ce qui arrive, `dto/response` pour ce qui repart).

Toutes les erreurs passent par le `GlobalExceptionHandler` dans `exception`. Comme ça le frontend reçoit toujours un message en français avec le bon code (400, 401, 403, 404 ou 409) au lieu d'une erreur technique.

La sécurité (JWT, rôles, CORS) est dans `security` et `config`, avec la config de Stripe et de Swagger. Il y a deux rôles : CLIENT et ADMIN.

## Lancer le projet en local

Il faut Java 21 et MySQL 8 (j'ai utilisé Laragon). Maven n'est pas nécessaire, le wrapper `mvnw` est fourni.

1. Créer une base de données vide appelée `legrandcinema`. Les tables sont créées automatiquement au premier lancement.
2. Copier le fichier `src/main/resources/application-secrets.properties.example` en `application-secrets.properties` dans le même dossier, puis le remplir :
   - l'adresse de la base, l'utilisateur et le mot de passe MySQL ;
   - une clé secrète pour le JWT ;
   - la clé secrète Stripe de test (elle commence par `sk_test_`) ;
   - les identifiants SMTP Brevo. Ils sont facultatifs : sans eux l'application démarre, mais les e-mails ne partent pas.

   Ce fichier contient des secrets, il est ignoré par Git et ne doit jamais être commité.
3. Lancer l'application :
   ```bash
   .\mvnw spring-boot:run
   ```
   (sous Linux ou Mac : `./mvnw spring-boot:run`)
4. L'API tourne sur http://localhost:8080 et la documentation Swagger est sur http://localhost:8080/swagger-ui.html.

Pour devenir administrateur, il faut créer un compte normal puis passer son rôle à `ADMIN` directement dans la table `utilisateurs`.

Pour payer en mode test, on utilise la carte Stripe `4242 4242 4242 4242`, avec une date dans le futur et n'importe quel code à 3 chiffres.

## Tests

```bash
.\mvnw clean test
```

Le projet contient plus de 170 tests : des tests unitaires des services avec Mockito, des tests des contrôleurs, de la sécurité JWT et un test de concurrence sur la réservation d'une même place.

Le rapport de couverture JaCoCo est ensuite dans `target/site/jacoco/index.html`.

## Les routes de l'API

Le détail de chaque route (paramètres, réponses, codes d'erreur) est dans Swagger. Voici la liste par thème, avec qui peut y accéder.

**Authentification** (public)
- `POST /api/auth/register` : créer un compte client
- `POST /api/auth/login` : se connecter et recevoir un token JWT
- `POST /api/auth/mot-de-passe-oublie` : recevoir un lien de réinitialisation par e-mail
- `POST /api/auth/reinitialiser-mot-de-passe` : choisir un nouveau mot de passe

**Films** (lecture publique, modification admin)
- `GET /api/films` : tous les films
- `GET /api/films/a-l-affiche` : les films qui ont des séances à venir
- `GET /api/films/{id}` : le détail d'un film
- `GET /api/films/{id}/seances` : les séances à venir d'un film
- `POST`, `PUT /api/films/{id}`, `DELETE /api/films/{id}` : ajouter, modifier, supprimer un film (admin)

**Salles et séances** (lecture publique, modification admin)
- `GET /api/salles`, `GET /api/salles/{id}`
- `GET /api/seances`, `GET /api/seances/{id}`
- `POST`, `PUT` et `DELETE` sur `/api/salles` et `/api/seances` (admin). Les places d'une séance sont créées automatiquement à sa création.

**Places** (client connecté)
- `GET /api/places/seance/{seanceId}` : le plan des places d'une séance
- `POST /api/places/{id}/verrouiller` : bloquer une place pendant 5 minutes
- `POST /api/places/{id}/liberer` : relâcher une place
- `POST /api/places/{id}/bloquer` et `/debloquer` : retirer une place de la vente ou la remettre (admin)

**Réservation et paiement** (client connecté)
- `POST /api/reservations` : créer une réservation avec les places verrouillées
- `POST /api/paiements/intention` : préparer le paiement Stripe
- `POST /api/paiements/confirmation` : confirmer le paiement, ce qui crée les billets et envoie l'e-mail

**Billets**
- `GET /api/billets/mes-billets` : les billets du client connecté
- `GET /api/billets/{id}/qrcode` : l'image du QR code, seulement pour le propriétaire du billet
- `POST /api/billets/scan` : contrôler un billet à l'entrée, un billet déjà utilisé est refusé (admin)

**Administration** (admin)
- `GET /api/admin/statistiques` : le taux de remplissage des séances
- `GET /api/admin/reservations` : les réservations en cours

Toutes les routes protégées attendent le token dans l'en-tête `Authorization`, sous la forme `Bearer` suivi d'un espace et du token reçu à la connexion.

## En ligne

- Site : https://legrandcinema-frontend.onrender.com
- API : https://legrandcinema-backend.onrender.com (l'adresse seule répond 401, c'est normal car aucune route n'existe à la racine ; pour tester, ouvrir par exemple https://legrandcinema-backend.onrender.com/api/films/a-l-affiche)
- Swagger : https://legrandcinema-backend.onrender.com/swagger-ui.html
- Identifiants de démonstration (admin et client) : dans le tableau des liens de mon dossier.

Le backend est hébergé sur l'offre gratuite de Render : après un moment sans visite, il peut mettre jusqu'à une minute à se réveiller.

## Organisation du travail avec Git

Branches :
- `main` : la version stable, publiée.
- `develop` : la branche d'intégration. Tout passe par elle avant d'arriver sur `main`.
- `feature/xxx` : une nouvelle fonctionnalité (ex : `feature/paiement-en-deux-etapes`).
- `fix/xxx` : une correction (ex : `fix/reservation-sans-doublon`).
- `refactor/xxx` : une réorganisation du code sans changer son comportement.
- `test/xxx` : l'ajout de tests (ex : `test/couverture-controleurs`).
- `docs/xxx` : la documentation (ex : `docs/swagger-descriptions`).
- `chore/xxx` : la configuration, les outils, le déploiement (ex : `chore/preparation-deploiement`).

Chaque branche part de `develop` et y revient par une Pull Request.

Messages de commit : `type: description en français`, par exemple `feat: ajout de l'entité Utilisateur`.
Les types utilisés sont `feat`, `fix`, `test`, `refactor`, `docs`, `config` et `chore`.

## Auteure

Katia Lakhdari
