pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                git branch: 'integration_medsadek',
                url: 'https://github.com/alinajjaa/Esprit-PIDEV-4SAE4-2026-COGNIVITA.git'
            }
        }

        stage('Run Unit Tests') {
            steps {
                sh '''
                    chmod +x content-service/mvnw
                    chmod +x tracking-service/mvnw
                    chmod +x dashboard-service/mvnw
                    chmod +x api-gateway/mvnw
                    chmod +x eureka-server/mvnw

                    cd content-service && ./mvnw test && cd ..
                    cd tracking-service && ./mvnw test && cd ..
                    cd dashboard-service && ./mvnw test && cd ..
                    cd api-gateway && ./mvnw test && cd ..
                    cd eureka-server && ./mvnw test && cd ..
                '''
            }
        }

        stage('Build Java Services') {
            steps {
                sh '''
                    cd content-service && ./mvnw clean package -DskipTests && cd ..
                    cd tracking-service && ./mvnw clean package -DskipTests && cd ..
                    cd dashboard-service && ./mvnw clean package -DskipTests && cd ..
                    cd api-gateway && ./mvnw clean package -DskipTests && cd ..
                    cd eureka-server && ./mvnw clean package -DskipTests && cd ..
                '''
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
                          -Dsonar.java.binaries=content-service/target/classes,tracking-service/target/classes,dashboard-service/target/classes,api-gateway/target/classes,eureka-server/target/classes \
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