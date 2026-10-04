package com.legrandcinema.service;

import com.legrandcinema.entity.Billet;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.exception.ResourceNotFoundException;
import com.legrandcinema.repository.BilletRepository;
import com.legrandcinema.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BilletServiceTest {

    @Mock
    private BilletRepository billetRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private QrCodeService qrCodeService;

    @InjectMocks
    private BilletService billetService;

    private Reservation reservation;
    private Billet billet;

    @BeforeEach
    void setUp() {
        Utilisateur proprietaire = new Utilisateur();
        proprietaire.setEmail("client@test.com");

        reservation = new Reservation();
        reservation.setId(1L);
        reservation.setUtilisateur(proprietaire);

        billet = new Billet();
        billet.setId(1L);
        billet.setReservation(reservation);
        billet.setQrCode("qr-code-test-123");
        billet.setScanne(false);
    }

    @Test
    void creerBillet_casNominal_creeLeBilletAvecQrCode() {
        when(billetRepository.existsByReservationId(1L)).thenReturn(false);
        when(billetRepository.save(any(Billet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Billet resultat = billetService.creerBillet(reservation);

        assertNotNull(resultat.getQrCode());
        assertFalse(resultat.getQrCode().isEmpty());
        assertFalse(resultat.isScanne());
        assertEquals(reservation, resultat.getReservation());
        verify(billetRepository, times(1)).save(any(Billet.class));
    }

    @Test
    void creerBillet_billetDejaExistant_leveException() {
        when(billetRepository.existsByReservationId(1L)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> billetService.creerBillet(reservation));

        assertEquals("Un billet existe déjà pour cette réservation", exception.getMessage());
        verify(billetRepository, never()).save(any(Billet.class));
    }

    @Test
    void scannerBillet_billetValide_marqueScanneEtRenvoieLeBillet() {
        when(billetRepository.findByQrCode("qr-code-test-123")).thenReturn(Optional.of(billet));
        when(billetRepository.save(any(Billet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Billet resultat = billetService.scannerBillet("qr-code-test-123");

        assertTrue(resultat.isScanne());
        verify(billetRepository, times(1)).save(any(Billet.class));
    }

    @Test
    void scannerBillet_qrCodeInconnu_leveResourceNotFound() {
        when(billetRepository.findByQrCode("qr-code-invalide")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> billetService.scannerBillet("qr-code-invalide"));

        assertEquals("Billet introuvable", exception.getMessage());
        verify(billetRepository, never()).save(any(Billet.class));
    }

    @Test
    void scannerBillet_billetDejaScanne_leveException() {
        billet.setScanne(true);
        when(billetRepository.findByQrCode("qr-code-test-123")).thenReturn(Optional.of(billet));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> billetService.scannerBillet("qr-code-test-123"));

        assertEquals("Billet déjà utilisé", exception.getMessage());
        verify(billetRepository, never()).save(any(Billet.class));
    }

    @Test
    void mesBillets_utilisateurConnu_renvoieSesBillets() {
        Utilisateur client = new Utilisateur();
        client.setId(5L);
        client.setEmail("client@test.com");

        when(utilisateurRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(billetRepository.findByReservationUtilisateurId(5L)).thenReturn(List.of(billet));

        List<Billet> resultat = billetService.mesBillets("client@test.com");

        assertEquals(1, resultat.size());
        assertEquals(billet, resultat.get(0));
        verify(billetRepository, times(1)).findByReservationUtilisateurId(5L);
    }

    @Test
    void mesBillets_aucunBillet_renvoieListeVide() {
        Utilisateur client = new Utilisateur();
        client.setId(5L);
        client.setEmail("client@test.com");

        when(utilisateurRepository.findByEmail("client@test.com")).thenReturn(Optional.of(client));
        when(billetRepository.findByReservationUtilisateurId(5L)).thenReturn(List.of());

        List<Billet> resultat = billetService.mesBillets("client@test.com");

        assertTrue(resultat.isEmpty());
    }

    @Test
    void mesBillets_utilisateurInconnu_leveResourceNotFound() {
        when(utilisateurRepository.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> billetService.mesBillets("inconnu@test.com"));

        assertEquals("Utilisateur introuvable", exception.getMessage());
        verify(billetRepository, never()).findByReservationUtilisateurId(anyLong());
    }

    @Test
    void obtenirImageQrCode_proprietaire_renvoieLImage() {
        byte[] fausseImage = {1, 2, 3};
        when(billetRepository.findById(1L)).thenReturn(Optional.of(billet));
        when(qrCodeService.genererQrCode("qr-code-test-123")).thenReturn(fausseImage);

        byte[] resultat = billetService.obtenirImageQrCode(1L, "client@test.com");

        assertArrayEquals(fausseImage, resultat);
        verify(qrCodeService, times(1)).genererQrCode("qr-code-test-123");
    }

    @Test
    void obtenirImageQrCode_billetInexistant_leveResourceNotFound() {
        when(billetRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> billetService.obtenirImageQrCode(99L, "client@test.com"));

        assertEquals("Billet introuvable", exception.getMessage());
        verify(qrCodeService, never()).genererQrCode(anyString());
    }

    @Test
    void obtenirImageQrCode_autreUtilisateur_leveResourceNotFound() {
        when(billetRepository.findById(1L)).thenReturn(Optional.of(billet));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> billetService.obtenirImageQrCode(1L, "curieux@test.com"));

        assertEquals("Billet introuvable", exception.getMessage());
        verify(qrCodeService, never()).genererQrCode(anyString());
    }
}