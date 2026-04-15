package louzaynej.pi.pi.exceptions;
public class RendezVousNotFoundException extends RuntimeException {
    public RendezVousNotFoundException(Long id) {
        super("RendezVous introuvable: " + id);
    }
}