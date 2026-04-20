package tn.esprit.cognivita.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.cognivita.entity.JournalEntry;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalRepository extends JpaRepository<JournalEntry, Long> {

    // Récupérer toutes les entrées triées par date décroissante
    List<JournalEntry> findAllByOrderByDateDesc();

    // Moyenne d'humeur avec filtre de dates optionnel
    @Query("SELECT AVG(j.mood) FROM JournalEntry j WHERE " +
            "(:fromDate IS NULL OR j.date >= :fromDate) AND " +
            "(:toDate IS NULL OR j.date <= :toDate)")
    Double averageMood(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    // Moyenne d'énergie avec filtre de dates
    @Query("SELECT AVG(j.energy) FROM JournalEntry j WHERE " +
            "(:fromDate IS NULL OR j.date >= :fromDate) AND " +
            "(:toDate IS NULL OR j.date <= :toDate)")
    Double averageEnergy(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    // Moyenne de stress avec filtre de dates
    @Query("SELECT AVG(j.stress) FROM JournalEntry j WHERE " +
            "(:fromDate IS NULL OR j.date >= :fromDate) AND " +
            "(:toDate IS NULL OR j.date <= :toDate)")
    Double averageStress(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    // Moyenne d'heures de sommeil avec filtre de dates
    @Query("SELECT AVG(j.sleepHours) FROM JournalEntry j WHERE " +
            "(:fromDate IS NULL OR j.date >= :fromDate) AND " +
            "(:toDate IS NULL OR j.date <= :toDate)")
    Double averageSleep(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    // Optionnel : Récupérer les entrées entre deux dates
    List<JournalEntry> findByDateBetweenOrderByDateDesc(LocalDate startDate, LocalDate endDate);
}