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

                    (cd content-service && ./mvnw test)
                    (cd tracking-service && ./mvnw test)
                    (cd dashboard-service && ./mvnw test)
                    (cd api-gateway && ./mvnw test)
                    (cd eureka-server && ./mvnw test)
                '''
            }
        }

        stage('Build Java Services') {
            steps {
                sh '''
                    (cd content-service && ./mvnw clean package -DskipTests)
                    (cd tracking-service && ./mvnw clean package -DskipTests)
                    (cd dashboard-service && ./mvnw clean package -DskipTests)
                    (cd api-gateway && ./mvnw clean package -DskipTests)
                    (cd eureka-server && ./mvnw clean package -DskipTests)
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

        stage('Build Docker Images') {
            steps {
                sh '''
                    docker build -t medsadek/eureka-server:latest ./eureka-server
                    docker build -t medsadek/api-gateway:latest ./api-gateway
                    docker build -t medsadek/content-service:latest ./content-service
                    docker build -t medsadek/tracking-service:latest ./tracking-service
                    docker build -t medsadek/dashboard-service:latest ./dashboard-service
                    docker build -t medsadek/frontend:latest ./frontend
                '''
            }
        }

        stage('Push Docker Images') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'docker-hub',
                    usernameVariable: 'DOCKERHUB_USERNAME',
                    passwordVariable: 'DOCKERHUB_PASSWORD'
                )]) {
                    sh '''
                        echo "$DOCKERHUB_PASSWORD" | docker login -u "$DOCKERHUB_USERNAME" --password-stdin

                        docker push medsadek/eureka-server:latest
                        docker push medsadek/api-gateway:latest
                        docker push medsadek/content-service:latest
                        docker push medsadek/tracking-service:latest
                        docker push medsadek/dashboard-service:latest
                        docker push medsadek/frontend:latest
                    '''
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh '''
                    kubectl apply --validate=false -k k8s
                    kubectl apply --validate=false -k k8s/monitoring

                    kubectl rollout restart deployment/eureka-server -n cognivita
                    kubectl rollout restart deployment/api-gateway -n cognivita
                    kubectl rollout restart deployment/content-service -n cognivita
                    kubectl rollout restart deployment/tracking-service -n cognivita
                    kubectl rollout restart deployment/dashboard-service -n cognivita
                    kubectl rollout restart deployment/frontend -n cognivita
                '''
            }
        }

        stage('Docker Compose Build') {
            steps {
                sh 'docker compose build'
            }
        }

        stage('Deploy Docker Compose') {
            steps {
                sh 'docker compose up -d'
            }
        }
    }
}