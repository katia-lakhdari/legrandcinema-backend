package com.legrandcinema.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationRequetesTest {

    private static ValidatorFactory fabrique;
    private static Validator validateur;

    @BeforeAll
    static void creerValidateur() {
        fabrique = Validation.buildDefaultValidatorFactory();
        validateur = fabrique.getValidator();
    }

    @AfterAll
    static void fermerValidateur() {
        fabrique.close();
    }

    private SeanceRequest creerSeanceValide() {
        SeanceRequest requete = new SeanceRequest();
        requete.setFilmId(1L);
        requete.setSalleId(1L);
        requete.setDateHeure(LocalDateTime.now().plusDays(1));
        requete.setPrix(new BigDecimal("9.50"));
        return requete;
    }

    private <T> String premierMessage(Set<ConstraintViolation<T>> erreurs) {
        return erreurs.iterator().next().getMessage();
    }

    @Test
    void seanceRequest_prixValide_aucuneErreur() {
        Set<ConstraintViolation<SeanceRequest>> erreurs = validateur.validate(creerSeanceValide());

        assertTrue(erreurs.isEmpty());
    }

    @Test
    void seanceRequest_prixTropPetit_estRefuse() {
        SeanceRequest requete = creerSeanceValide();
        requete.setPrix(new BigDecimal("0.10"));

        Set<ConstraintViolation<SeanceRequest>> erreurs = validateur.validate(requete);

        assertEquals(1, erreurs.size());
        assertEquals("Le prix doit être d'au moins 0,50 €", premierMessage(erreurs));
    }

    @Test
    void seanceRequest_prixAvecTroisDecimales_estRefuse() {
        SeanceRequest requete = creerSeanceValide();
        requete.setPrix(new BigDecimal("9.999"));

        Set<ConstraintViolation<SeanceRequest>> erreurs = validateur.validate(requete);

        assertEquals(1, erreurs.size());
        assertEquals("Le prix doit avoir au plus 3 chiffres avant la virgule et 2 après", premierMessage(erreurs));
    }

    @Test
    void salleRequest_capaciteTropGrande_estRefusee() {
        SalleRequest requete = new SalleRequest();
        requete.setNom("Salle 1");
        requete.setType("STANDARD");
        requete.setCapacite(100000);

        Set<ConstraintViolation<SalleRequest>> erreurs = validateur.validate(requete);

        assertEquals(1, erreurs.size());
        assertEquals("La capacité ne doit pas dépasser 520 places (26 rangées de 20)", premierMessage(erreurs));
    }

    @Test
    void inscriptionRequest_motDePasseTropLong_estRefuse() {
        InscriptionRequest requete = new InscriptionRequest();
        requete.setPrenom("Katia");
        requete.setNom("Lakhdari");
        requete.setEmail("katia@legrandcinema.com");
        requete.setMotDePasse("a".repeat(73));

        Set<ConstraintViolation<InscriptionRequest>> erreurs = validateur.validate(requete);

        assertEquals(1, erreurs.size());
        assertEquals("Le mot de passe doit contenir entre 8 et 72 caractères", premierMessage(erreurs));
    }

    @Test
    void filmRequest_descriptionTropLongue_estRefusee() {
        FilmRequest requete = new FilmRequest();
        requete.setTitre("Le Grand Bleu");
        requete.setGenre("Drame");
        requete.setDuree(168);
        requete.setAffiche("https://image.tmdb.org/t/p/w342/affiche.jpg");
        requete.setDescription("a".repeat(1001));

        Set<ConstraintViolation<FilmRequest>> erreurs = validateur.validate(requete);

        assertEquals(1, erreurs.size());
        assertEquals("La description ne doit pas dépasser 1000 caractères", premierMessage(erreurs));
    }
}
