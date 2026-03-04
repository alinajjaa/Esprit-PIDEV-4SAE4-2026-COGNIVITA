package louzaynej.pi.pi.dto;



import louzaynej.pi.pi.model.Room;
import louzaynej.pi.pi.model.Nurse;
import louzaynej.pi.pi.model.Medication;

import java.util.List;

public record RendezVousCareAssignRequest(
        Room chambre,
        Nurse infermiere,
        List<Medication> medicaments
) {}