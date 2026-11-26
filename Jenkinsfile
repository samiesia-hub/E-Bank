pipeline {
    agent any

    tools {
        jdk    'jdk21'
        nodejs 'node20'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 40, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        SONAR_HOST_URL = 'http://sonarqube:9000'
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Recuperation du projet depuis GitHub'
                checkout scm
            }
        }

        stage('Tests') {
            parallel {

                stage('Backend - JUnit + JaCoCo') {
                    steps {
                        dir('pro') {
                            sh 'chmod +x mvnw'
                            sh './mvnw -B test'
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true,
                                  testResults: 'pro/target/surefire-reports/*.xml'
                        }
                    }
                }

                stage('Frontend - Vitest') {
                    steps {
                        dir('e-bank-frontend') {
                            sh 'npm ci'
                            sh 'npm run lint'
                            sh 'npm run test:ci'
                            sh 'npm run build'
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true,
                                  testResults: 'e-bank-frontend/reports/junit.xml'
                        }
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo 'Analyse de qualite du code'
                withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                    sh 'bash devops/sonarqube/analyze.sh'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
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
            when { branch 'main' }
            steps {
                echo 'Deploiement de Orabank'
                sh 'docker compose up -d --remove-orphans'
            }
        }

        stage('Smoke Test') {
            when { branch 'main' }
            steps {
                sh '''
                  for i in $(seq 1 20); do
                    if curl -sf http://host.docker.internal:8081/actuator/health | grep -q UP; then
                      echo "Application UP"; exit 0
                    fi
                    echo "Attente... ($i/20)"; sleep 5
                  done
                  echo "Healthcheck en echec"; exit 1
                '''
            }
        }
    }

    post {
        success { echo 'Pipeline CI/CD termine avec succes !' }
        failure { echo 'Echec du pipeline. Consulte les logs Jenkins.' }
        always  { echo 'Fin du pipeline.' }
    }
}
