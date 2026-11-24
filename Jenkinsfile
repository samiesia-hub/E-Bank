pipeline {

    agent any

    environment {
        SONAR_HOST_URL = 'http://localhost:9000'
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Recuperation du projet depuis GitHub'
                checkout scm
            }
        }

        stage('Backend - Build & Tests') {
            steps {
                echo 'Compilation et tests Spring Boot'

                dir('pro') {
                    sh 'mvn -B clean verify'
                }
            }
        }

        stage('Frontend - Build & Lint') {
            steps {
                echo 'Installation et verification du frontend'

                dir('e-bank-frontend') {
                    sh 'npm ci'
                    sh 'npm run lint'
                    sh 'npm run build'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo 'Analyse de qualite du code'

                withCredentials([
                    string(
                        credentialsId: 'sonar-token',
                        variable: 'SONAR_TOKEN'
                    )
                ]) {
                    sh '''
                        bash devops/sonarqube/analyze.sh
                    '''
                }
            }
        }

        stage('Docker Build') {
            steps {
                echo 'Construction des images Docker'

                sh 'docker compose build'
            }
        }

        stage('Deploy') {
            steps {
                echo 'Deploiement de Orabank'

                sh 'docker compose up -d'
            }
        }
    }

    post {
        success {
            echo 'Pipeline CI/CD termine avec succes !'
        }

        failure {
            echo 'Echec du pipeline. Consulte les logs Jenkins.'
        }

        always {
            echo 'Fin du pipeline.'
        }
    }
}
