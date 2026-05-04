/*
 * ============================================================
 *  COGNIVITA — Jenkinsfile Principal (Racine)
 *  CI + CD automatique pour : pi-rdv-service et plansuivi-service
 * ============================================================
 *
 * Architecture des pipelines :
 *  - Ce fichier = entrée principale (CI) pour les 2 services en parallèle
 *  - Le CD est déclenché automatiquement si le CI réussit
 *  - Fichiers séparés par service :
 *      pi/pi/Jenkinsfile-CI         → CI service Rendez-vous (port 8091)
 *      pi/pi/Jenkinsfile-CD         → CD service Rendez-vous
 *      planSuivi/planSuivi/Jenkinsfile-CI  → CI service Plan Suivi (port 8092)
 *      planSuivi/planSuivi/Jenkinsfile-CD  → CD service Plan Suivi
 */

pipeline {
    agent any

    tools {
        jdk 'jdk17'
        maven 'maven3'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    triggers {
        githubPush()
    }

    environment {
        PI_DIR       = 'pi/pi'
        PLANSUIVI_DIR = 'planSuivi/planSuivi'
    }

    stages {

        // ─────────────────────────────────────────────
        //  ÉTAPE 1 : Checkout
        // ─────────────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm
                echo "✅ Sources récupérées"
            }
        }

        // ─────────────────────────────────────────────
        //  ÉTAPE 2 : CI — Tests unitaires (parallèles)
        // ─────────────────────────────────────────────
        stage('CI — Unit Tests') {
            parallel {

                stage('PI RDV — Tests') {
                    steps {
                        dir("${PI_DIR}") {
                            script {
                                if (isUnix()) {
                                    sh 'mvn -B clean test'
                                } else {
                                    bat 'mvn -B clean test'
                                }
                            }
                        }
                    }
                    post {
                        always {
                            junit testResults: 'pi/pi/target/surefire-reports/*.xml',
                                  allowEmptyResults: true
                        }
                    }
                }

                stage('Plan Suivi — Tests') {
                    steps {
                        dir("${PLANSUIVI_DIR}") {
                            script {
                                if (isUnix()) {
                                    sh 'mvn -B clean test'
                                } else {
                                    bat 'mvn -B clean test'
                                }
                            }
                        }
                    }
                    post {
                        always {
                            junit testResults: 'planSuivi/planSuivi/target/surefire-reports/*.xml',
                                  allowEmptyResults: true
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────
        //  ÉTAPE 3 : CI — Qualité du code (SonarQube)
        // ─────────────────────────────────────────────
        stage('CI — SonarQube Analysis') {
            parallel {

                stage('PI RDV — Sonar') {
                    steps {
                        dir("${PI_DIR}") {
                            withSonarQubeEnv('SonarQube') {
                                script {
                                    if (isUnix()) {
                                        sh 'mvn -B verify sonar:sonar -Dsonar.projectKey=pi-rdv-service -Dsonar.projectName=pi-rdv-service'
                                    } else {
                                        bat 'mvn -B verify sonar:sonar -Dsonar.projectKey=pi-rdv-service -Dsonar.projectName=pi-rdv-service'
                                    }
                                }
                            }
                        }
                    }
                }

                stage('Plan Suivi — Sonar') {
                    steps {
                        dir("${PLANSUIVI_DIR}") {
                            withSonarQubeEnv('SonarQube') {
                                script {
                                    if (isUnix()) {
                                        sh 'mvn -B verify sonar:sonar -Dsonar.projectKey=plansuivi-service -Dsonar.projectName=plansuivi-service'
                                    } else {
                                        bat 'mvn -B verify sonar:sonar -Dsonar.projectKey=plansuivi-service -Dsonar.projectName=plansuivi-service'
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────
        //  ÉTAPE 4 : Quality Gate (SonarQube)
        // ─────────────────────────────────────────────
        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // ─────────────────────────────────────────────
        //  ÉTAPE 5 : CD — Build JARs (parallèle)
        // ─────────────────────────────────────────────
        stage('CD — Package') {
            parallel {

                stage('PI RDV — Package') {
                    steps {
                        dir("${PI_DIR}") {
                            script {
                                if (isUnix()) {
                                    sh 'mvn -B clean package -DskipTests'
                                } else {
                                    bat 'mvn -B clean package -DskipTests'
                                }
                            }
                        }
                    }
                }

                stage('Plan Suivi — Package') {
                    steps {
                        dir("${PLANSUIVI_DIR}") {
                            script {
                                if (isUnix()) {
                                    sh 'mvn -B clean package -DskipTests'
                                } else {
                                    bat 'mvn -B clean package -DskipTests'
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────
        //  ÉTAPE 6 : CD — Archive des artefacts
        // ─────────────────────────────────────────────
        stage('CD — Archive') {
            steps {
                archiveArtifacts artifacts: 'pi/pi/target/*.jar,planSuivi/planSuivi/target/*.jar',
                                 fingerprint: true,
                                 allowEmptyArchive: true
                echo "📦 JARs archivés avec succès"
            }
        }

        // ─────────────────────────────────────────────
        //  ÉTAPE 7 : CD — Déploiement (Windows/Linux)
        // ─────────────────────────────────────────────
        stage('CD — Deploy') {
            parallel {

                stage('PI RDV — Deploy (port 8091)') {
                    steps {
                        script {
                            if (isUnix()) {
                                sh '''
                                    fuser -k 8091/tcp || true
                                    sleep 2
                                    nohup java -jar pi/pi/target/pi-0.0.1-SNAPSHOT.jar \
                                        --server.port=8091 > /tmp/pi-service.log 2>&1 &
                                    echo "PI Service started"
                                '''
                            } else {
                                bat '''
                                    @echo off
                                    FOR /F "tokens=5" %%P IN ('netstat -ano ^| findstr ":8091 "') DO taskkill /PID %%P /F 2>nul
                                    timeout /t 2 /nobreak >nul
                                    start "" /B java -jar pi\\pi\\target\\pi-0.0.1-SNAPSHOT.jar --server.port=8091
                                    echo PI RDV Service started on port 8091
                                '''
                            }
                        }
                    }
                }

                stage('Plan Suivi — Deploy (port 8092)') {
                    steps {
                        script {
                            if (isUnix()) {
                                sh '''
                                    fuser -k 8092/tcp || true
                                    sleep 2
                                    nohup java -jar planSuivi/planSuivi/target/planSuivi-0.0.1-SNAPSHOT.jar \
                                        --server.port=8092 > /tmp/plansuivi-service.log 2>&1 &
                                    echo "PlanSuivi Service started"
                                '''
                            } else {
                                bat '''
                                    @echo off
                                    FOR /F "tokens=5" %%P IN ('netstat -ano ^| findstr ":8092 "') DO taskkill /PID %%P /F 2>nul
                                    timeout /t 2 /nobreak >nul
                                    start "" /B java -jar planSuivi\\planSuivi\\target\\planSuivi-0.0.1-SNAPSHOT.jar --server.port=8092
                                    echo Plan Suivi Service started on port 8092
                                '''
                            }
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────
        //  ÉTAPE 8 : Health Check
        // ─────────────────────────────────────────────
        stage('CD — Health Check') {
            steps {
                script {
                    sleep(time: 30, unit: 'SECONDS')
                    if (isUnix()) {
                        sh '''
                            echo "Checking PI RDV..."
                            curl -sf http://localhost:8091/actuator/health || echo "PI: Still starting..."
                            echo "Checking PlanSuivi..."
                            curl -sf http://localhost:8092/actuator/health || echo "PlanSuivi: Still starting..."
                        '''
                    } else {
                        bat '''
                            powershell -Command "try { (Invoke-WebRequest -Uri http://localhost:8091/actuator/health -UseBasicParsing).StatusCode | Out-Null; Write-Host 'PI RDV: OK' } catch { Write-Host 'PI RDV: Still starting...' }"
                            powershell -Command "try { (Invoke-WebRequest -Uri http://localhost:8092/actuator/health -UseBasicParsing).StatusCode | Out-Null; Write-Host 'PlanSuivi: OK' } catch { Write-Host 'PlanSuivi: Still starting...' }"
                        '''
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    //  POST — Rapports & Notifications
    // ─────────────────────────────────────────────
    post {
        success {
            echo """
            ╔══════════════════════════════════════════╗
            ║  ✅  CI/CD RÉUSSI — COGNIVITA Platform   ║
            ║  PI RDV  → http://localhost:8091          ║
            ║  PlanSuivi → http://localhost:8092        ║
            ╚══════════════════════════════════════════╝
            """
        }
        failure {
            echo "❌ Pipeline CI/CD échoué — Vérifiez les tests unitaires, SonarQube ou le packaging"
        }
        always {
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
            cleanWs()
        }
    }
}
