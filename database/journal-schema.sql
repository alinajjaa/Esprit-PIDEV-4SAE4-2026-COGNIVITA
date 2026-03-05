-- =====================================================
-- COGNIVITA - Mood Journal Module
-- =====================================================

USE cognivita_db;

-- -----------------------------------------------------
-- 1. Supprimer la table si elle existe
-- -----------------------------------------------------
DROP TABLE IF EXISTS journal_entries;

-- -----------------------------------------------------
-- 2. Créer la table journal_entries
-- -----------------------------------------------------
CREATE TABLE journal_entries (
                                 id INT PRIMARY KEY AUTO_INCREMENT,
                                 user_id INT NOT NULL,
                                 entry_date DATE NOT NULL,
                                 mood INT NOT NULL CHECK (mood BETWEEN 1 AND 5),
                                 energy INT NOT NULL CHECK (energy BETWEEN 1 AND 5),
                                 stress INT NOT NULL CHECK (stress BETWEEN 1 AND 5),
                                 sleep_hours DECIMAL(3,1) NOT NULL,
                                 activities TEXT,
                                 notes TEXT,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                 INDEX idx_user (user_id),
                                 INDEX idx_date (entry_date),
                                 UNIQUE KEY unique_user_entry (user_id, entry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 3. Insérer des données d'exemple
-- -----------------------------------------------------
INSERT INTO journal_entries (user_id, entry_date, mood, energy, stress, sleep_hours, activities, notes) VALUES
                                                                                                            (1, CURDATE(), 4, 3, 2, 7.5, '🧠 Mémoire,🚶 Promenade', 'Bonne journée'),
                                                                                                            (1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 5, 4, 1, 8.0, '🧠 Mémoire,📚 Lecture', 'Excellente journée'),
                                                                                                            (1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 3, 2, 4, 5.5, '🧠 Mémoire', 'Journée fatigante'),
                                                                                                            (1, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 4, 4, 2, 7.0, '🧘 Méditation,📚 Lecture', 'Calme et productif'),
                                                                                                            (1, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 5, 5, 1, 8.5, '🎵 Musique,🚶 Promenade', 'Super journée !');

-- -----------------------------------------------------
-- 4. Vérifier les données
-- -----------------------------------------------------
SELECT '=== JOURNAL ENTRIES ===' as '';
SELECT * FROM journal_entries ORDER BY entry_date DESC;