-- =====================================================
-- COGNIVITA - Base de données complète
-- =====================================================

USE cognivita_db;

-- =====================================================
-- PARTIE 1: ACTIVITÉS COGNITIVES
-- =====================================================

DROP TABLE IF EXISTS cognitive_activities;

CREATE TABLE cognitive_activities (
                                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                      title VARCHAR(255) NOT NULL,
                                      description TEXT,
                                      type VARCHAR(50) NOT NULL,
                                      difficulty_level VARCHAR(20) NOT NULL,
                                      content TEXT,
                                      time_limit INT,
                                      max_score INT,
                                      instructions TEXT,
                                      image_url VARCHAR(500),
                                      is_active BOOLEAN DEFAULT TRUE,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insertion des activités
INSERT INTO cognitive_activities (title, description, type, difficulty_level, content, time_limit, max_score, instructions) VALUES
                                                                                                                                ('Rappel de mots', 'Mémorisez une liste de mots', 'MEMORY', 'EASY', '{"words": ["maison", "arbre", "voiture", "livre", "soleil"]}', 120, 5, 'Mémorisez ces mots'),
                                                                                                                                ('Test de Stroop', 'Dites la couleur du mot', 'ATTENTION', 'EASY', '{"items": [{"word": "ROUGE", "color": "blue"}]}', 150, 10, 'Dites la couleur'),
                                                                                                                                ('Suite logique', 'Trouvez le nombre suivant', 'LOGIC', 'EASY', '{"sequences": [[2, 4, 6, 8]]}', 180, 5, 'Trouvez le nombre suivant');

-- =====================================================
-- PARTIE 2: JOURNAL D'HUMEUR
-- =====================================================

DROP TABLE IF EXISTS journal_entries;

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
                                 INDEX idx_date (entry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insertion des entrées du journal
INSERT INTO journal_entries (user_id, entry_date, mood, energy, stress, sleep_hours, activities, notes) VALUES
                                                                                                            (1, CURDATE(), 4, 3, 2, 7.5, '🧠 Mémoire,🚶 Promenade', 'Bonne journée'),
                                                                                                            (1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 5, 4, 1, 8.0, '🧠 Mémoire,📚 Lecture', 'Excellente journée');

-- =====================================================
-- VÉRIFICATION
-- =====================================================
SELECT '=== ACTIVITÉS COGNITIVES ===' as '';
SELECT * FROM cognitive_activities;

SELECT '=== JOURNAL ENTRIES ===' as '';
SELECT * FROM journal_entries;