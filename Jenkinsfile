pipeline {
    agent any


    stages {

        stage('Checkout') {
            steps {
                git branch: 'integration_medsadek',
                url: 'https://github.com/alinajjaa/Esprit-PIDEV-4SAE4-2026-COGNIVITA.git'
            }
        }

        stage('SonarQube Analysis') {
            environment {
                SONAR_SCANNER_HOME = tool 'sonar-scanner'
            }
            steps {
                withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                    sh '''
                        $SONAR_SCANNER_HOME/bin/sonar-scanner \
                          -Dsonar.projectKey=cognivita-integration \
                          -Dsonar.projectName="COGNIVITA Integration" \
                          -Dsonar.host.url=http://192.168.56.10:9000 \
                          -Dsonar.token=$SONAR_TOKEN \
                          -Dsonar.sources=. \
                          -Dsonar.exclusions=**/target/**,**/node_modules/**,**/dist/**,**/.angular/**,**/.git/**,**/venv/**,**/*.7z
                    '''
                }
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
