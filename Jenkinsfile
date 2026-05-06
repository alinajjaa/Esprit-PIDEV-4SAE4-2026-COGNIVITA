pipeline {
    agent any


    stages {

        stage('Checkout') {
            steps {
                git branch: 'integration_medsadek',
                url: 'https://github.com/alinajjaa/Esprit-PIDEV-4SAE4-2026-COGNIVITA.git'
            }
        }

        stage('Docker Compose Build') {
            steps {
                sh 'docker compose build'
            }
        }

        stage('Deploy') {
            steps {
                sh 'docker compose up -d'
            }
        }
    }
}
