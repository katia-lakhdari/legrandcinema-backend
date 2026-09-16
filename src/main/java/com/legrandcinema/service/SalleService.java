package com.legrandcinema.service;

import com.legrandcinema.entity.Salle;
import com.legrandcinema.repository.SalleRepository;
import com.legrandcinema.dto.request.SalleRequest;
import com.legrandcinema.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SalleService {

    @Autowired
    private SalleRepository salleRepository;

    public List<Salle> listerToutesLesSalles() {
        return salleRepository.findAll();
    }

    public Salle trouverParId(Long id) {
        return salleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable"));
    }

    public Salle creerSalle(SalleRequest request) {
        Salle salle = new Salle();
        salle.setNom(request.getNom());
        salle.setType(convertirType(request.getType()));
        salle.setCapacite(request.getCapacite());
        return salleRepository.save(salle);
    }

    public Salle modifierSalle(Long id, SalleRequest request) {
        Salle salle = trouverParId(id);
        salle.setNom(request.getNom());
        salle.setType(convertirType(request.getType()));
        salle.setCapacite(request.getCapacite());
        return salleRepository.save(salle);
    }

    public void supprimerSalle(Long id) {
        Salle salle = trouverParId(id);
        salleRepository.delete(salle);
    }

    private Salle.TypeSalle convertirType(String type) {
        try {
            return Salle.TypeSalle.valueOf(type);
        } catch (IllegalArgumentException exception) {
            throw new RuntimeException("Type de salle invalide : " + type
                    + " (valeurs acceptées : PREMIUM_IMAX, STANDARD, VIP, EVENEMENTIELLE)");
        }
    }
}