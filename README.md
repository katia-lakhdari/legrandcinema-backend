# Le Grand Cinéma

Application de réservation de places de cinéma en ligne. 
Architecture 3-tiers : Front(HTML/CSS/JS) -> Back (Spring Boot) -> BDD (MySQL)

## Convention de nommage des branches

- `main` -> Code stable, déployé. 
- `develop` -> Branche d'intégration, toutes les features y sont fusionnées avant `main`
- `feature/xxx` -> Une branche par fonctionnalité (ex: `feature/select-seats`)
- `fix/xxx` -> une branche par correction de bug (ex: `fix/double-reservation`)

## Stack technique 
- Backend : Spring Boot 4.1, Java 21
- Base de données : MySQL
- Sécurité : Spring Security + JWT
- Paiement : Stripe 