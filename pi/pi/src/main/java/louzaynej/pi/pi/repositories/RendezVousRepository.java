package louzaynej.pi.pi.repositories;

import louzaynej.pi.pi.model.RendezVous;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RendezVousRepository extends JpaRepository <RendezVous,Long> {
    List<RendezVous> findByMedecinId(Long medecinId);

    List<RendezVous> findByPatientId(Long patientId);

    //  conflict: same doctor, same dateHeure
    boolean existsByMedecinIdAndDateHeure(Long medecinId, LocalDateTime dateHeure);

    // OPTIONAL (stronger): also prevent patient same time
    boolean existsByPatientIdAndDateHeure(Long patientId, LocalDateTime dateHeure);List<RendezVous> findByMedecinIdAndDateHeureBetween(
            Long medecinId,
            LocalDateTime from,
            LocalDateTime to
    );

    List<RendezVous> findByPatientIdAndDateHeureBetween(
            Long patientId,
            LocalDateTime from,
            LocalDateTime to
    );

    // ---- Eager-loading versions to prevent N+1 queries ----
    @Query("""
        SELECT DISTINCT r
        FROM RendezVous r
        LEFT JOIN FETCH r.medecin
        LEFT JOIN FETCH r.patient
        WHERE r.medecin.id = :medecinId
        AND r.dateHeure BETWEEN :from AND :to
        ORDER BY r.dateHeure ASC
    """)
    List<RendezVous> findByMedecinIdAndDateHeureBetweenEager(
            @Param("medecinId") Long medecinId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT DISTINCT r
        FROM RendezVous r
        LEFT JOIN FETCH r.medecin
        LEFT JOIN FETCH r.patient
        WHERE r.patient.id = :patientId
        AND r.dateHeure BETWEEN :from AND :to
        ORDER BY r.dateHeure ASC
    """)
    List<RendezVous> findByPatientIdAndDateHeureBetweenEager(
            @Param("patientId") Long patientId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // ---- Eager-loading for list methods to prevent serialization errors ----
    @Query("""
        SELECT DISTINCT r
        FROM RendezVous r
        LEFT JOIN FETCH r.medecin
        LEFT JOIN FETCH r.patient
        ORDER BY r.dateHeure ASC
    """)
    List<RendezVous> findAllEager();

    @Query("""
        SELECT DISTINCT r
        FROM RendezVous r
        LEFT JOIN FETCH r.medecin
        LEFT JOIN FETCH r.patient
        WHERE r.medecin.id = :medecinId
        ORDER BY r.dateHeure ASC
    """)
    List<RendezVous> findByMedecinIdEager(@Param("medecinId") Long medecinId);

    @Query("""
        SELECT DISTINCT r
        FROM RendezVous r
        LEFT JOIN FETCH r.medecin
        LEFT JOIN FETCH r.patient
        WHERE r.patient.id = :patientId
        ORDER BY r.dateHeure ASC
    """)
    List<RendezVous> findByPatientIdEager(@Param("patientId") Long patientId);

    // ---- KPI counts
    long countByMedecinIdAndDateHeureBetween(Long medecinId, LocalDateTime from, LocalDateTime to);

    @Query("""
        select count(r) from RendezVous r
        where r.medecin.id = :medecinId
          and r.dateHeure >= :now
          and r.dateHeure < :to
    """)
    long countUpcoming(@Param("medecinId") Long medecinId,
                       @Param("now") LocalDateTime now,
                       @Param("to") LocalDateTime to);

    @Query("""
        select count(distinct r.patient.id) from RendezVous r
        where r.medecin.id = :medecinId
          and r.dateHeure between :from and :to
    """)
    long countUniquePatients(@Param("medecinId") Long medecinId,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to);

    // ---- by status
    @Query("""
        select r.status as status, count(r) as count
        from RendezVous r
        where r.medecin.id = :medecinId
          and r.dateHeure between :from and :to
        group by r.status
    """)
    List<StatusCountView> countByStatus(@Param("medecinId") Long medecinId,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    // ---- per day
    @Query("""
        select function('date', r.dateHeure) as day, count(r) as count
        from RendezVous r
        where r.medecin.id = :medecinId
          and r.dateHeure between :from and :to
        group by function('date', r.dateHeure)
        order by function('date', r.dateHeure)
    """)
    List<DayCountView> countPerDay(@Param("medecinId") Long medecinId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    // ---- per hour (0..23)
    @Query("""
        select function('hour', r.dateHeure) as hour, count(r) as count
        from RendezVous r
        where r.medecin.id = :medecinId
          and r.dateHeure between :from and :to
        group by function('hour', r.dateHeure)
        order by function('hour', r.dateHeure)
    """)
    List<HourCountView> countPerHour(@Param("medecinId") Long medecinId,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);

    // ---- per weekday (1..7 in many DBs; depends)
    @Query("""
        select function('dayofweek', r.dateHeure) as weekday, count(r) as count
        from RendezVous r
        where r.medecin.id = :medecinId
          and r.dateHeure between :from and :to
        group by function('dayofweek', r.dateHeure)
        order by function('dayofweek', r.dateHeure)
    """)
    List<WeekdayCountView> countPerWeekday(@Param("medecinId") Long medecinId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    // ---- rooms
    @Query("""
        select cast(r.chambre as string) as name, count(r) as count
        from RendezVous r
        where r.medecin.id = :medecinId
          and r.dateHeure between :from and :to
          and r.chambre is not null
        group by r.chambre
        order by count(r) desc
    """)
    List<NameCountView> topRooms(@Param("medecinId") Long medecinId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);

    // ---- nurses
    @Query("""
        select cast(r.infermiere as string) as name, count(r) as count
        from RendezVous r
        where r.medecin.id = :medecinId
          and r.dateHeure between :from and :to
          and r.infermiere is not null
        group by r.infermiere
        order by count(r) desc
    """)
    List<NameCountView> topNurses(@Param("medecinId") Long medecinId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);

    // ---- medications (ElementCollection) : join needed
    @Query("""
        select cast(m as string) as name, count(m) as count
        from RendezVous r join r.medicaments m
        where r.medecin.id = :medecinId
          and r.dateHeure between :from and :to
        group by m
        order by count(m) desc
    """)
    List<NameCountView> topMedications(@Param("medecinId") Long medecinId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    // ---- top patients
    @Query("""
        select r.patient.nomPatient as name, count(r) as count
        from RendezVous r
        where r.medecin.id = :medecinId
          and r.dateHeure between :from and :to
        group by r.patient.nomPatient
        order by count(r) desc
    """)
    List<NameCountView> topPatients(@Param("medecinId") Long medecinId,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);

}
