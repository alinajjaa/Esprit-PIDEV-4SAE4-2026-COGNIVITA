pipeline {
    agent any

    tools {
        jdk 'jdk17'
        maven 'maven3'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        PI_DIR = 'pi/pi'
        PLANSUIVI_DIR = 'planSuivi/planSuivi'
    }

    triggers {
        githubPush()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('CI - Unit Tests') {
            parallel {
                stage('PI - Tests') {
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
                }
                stage('PlanSuivi - Tests') {
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
                }
            }
        }

        stage('CD - Build Artifacts') {
            when {
                branch 'main'
            }
            parallel {
                stage('PI - Package') {
                    steps {
                        dir("${PI_DIR}") {
                            script {
                                if (isUnix()) {
                                    sh 'mvn -B -DskipTests package'
                                } else {
                                    bat 'mvn -B -DskipTests package'
                                }
                            }
                        }
                    }
                }
                stage('PlanSuivi - Package') {
                    steps {
                        dir("${PLANSUIVI_DIR}") {
                            script {
                                if (isUnix()) {
                                    sh 'mvn -B -DskipTests package'
                                } else {
                                    bat 'mvn -B -DskipTests package'
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('CD - Archive Artifacts') {
            when {
                branch 'main'
            }
            steps {
                archiveArtifacts artifacts: 'pi/pi/target/*.jar,planSuivi/planSuivi/target/*.jar', fingerprint: true
            }
        }
    }

    post {
        success {
            echo 'CI/CD Jenkins termine avec succes pour PI et PlanSuivi.'
        }
        failure {
            echo 'Echec du pipeline. Verifie les tests unitaires ou le packaging.'
        }
        always {
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
        }
    }
}
