package louzaynej.pi.pi.controllers;

import louzaynej.pi.pi.model.Medication;
import louzaynej.pi.pi.model.Nurse;
import louzaynej.pi.pi.model.Room;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/options")
public class OptionsController {

    @GetMapping("/rooms")
    public Room[] rooms() { return Room.values(); }

    @GetMapping("/nurses")
    public Nurse[] nurses() { return Nurse.values(); }

    @GetMapping("/medications")
    public Medication[] medications() { return Medication.values(); }
}
