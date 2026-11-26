pipeline {
    agent any

    tools {
        jdk    'jdk21'
        nodejs 'NodeJS-22'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 40, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

        // 1. CHECKOUT
        stage('Checkout') {
            steps {
                echo 'Recuperation du projet depuis GitHub'
                checkout scm
            }
        }

        // 2. TESTS (backend et frontend en parallele)
        stage('Tests') {
            parallel {

                stage('Backend - JUnit + JaCoCo') {
                    steps {
                        dir('pro') {
                            sh 'chmod +x mvnw'
                            sh './mvnw -B clean test'
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true,
                                  testResults: 'pro/target/surefire-reports/*.xml'
                        }
                    }
                }

                stage('Frontend - Lint + Vitest') {
                    steps {
                        dir('e-bank-frontend') {
                            sh 'npm ci'
                            sh 'npm run lint'
                            sh 'npm run test:ci'
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

        // 3. BUILD (apres les tests, pour ne pas perdre le rapport JaCoCo)
        stage('Build') {
            parallel {
                stage('Build Backend') {
                    steps {
                        dir('pro') {
                            // pas de "clean" : on garde target/site/jacoco et target/classes
                            sh './mvnw -B package -DskipTests'
                        }
                    }
                }
                stage('Build Frontend') {
                    steps {
                        dir('e-bank-frontend') {
                            sh 'npm run build'
                        }
                    }
                }
            }
        }

        // 4. SONARQUBE
        stage('SonarQube Analysis') {
            steps {
                echo 'Analyse de qualite du code'
                withSonarQubeEnv('sonarqube') {
                    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                        sh 'bash devops/sonarqube/analyze.sh'
                    }
                }
            }
        }

        // 5. QUALITY GATE
        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // 6. DOCKER BUILD
        stage('Docker Build') {
            steps {
                echo 'Construction des images Docker'
                sh 'docker compose build'
            }
        }

        // 7. DEPLOY (uniquement sur main)
        stage('Deploy') {
            when { branch 'main' }
            steps {
                echo 'Deploiement de Orabank'
                sh 'docker compose up -d --remove-orphans'
            }
        }

        // 8. SMOKE TEST (uniquement sur main)
        stage('Smoke Test') {
            when { branch 'main' }
            steps {
                sh '''
                    for i in $(seq 1 20); do
                        if curl -sf http://host.docker.internal:8081/actuator/health | grep -q UP; then
                            echo "Application UP"
                            exit 0
                        fi
                        echo "Attente... ($i/20)"
                        sleep 5
                    done
                    echo "Healthcheck en echec"
                    exit 1
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
