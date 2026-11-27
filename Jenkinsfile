pipeline {
    agent any

    tools {
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

        // 3. BUILD (sans clean pour garder jacoco et target/classes)
        stage('Build') {
            parallel {
                stage('Build Backend') {
                    steps {
                        dir('pro') {
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

        // 4. SONARQUBE (scanner installe dans Jenkins, sans Docker)
        stage('SonarQube Analysis') {
            environment {
                SCANNER_HOME = tool 'sonar-scanner'
            }
            steps {
                echo 'Analyse de qualite du code'
                withSonarQubeEnv('sonarqube') {
                    sh '''
                      "$SCANNER_HOME/bin/sonar-scanner" \
                        -Dsonar.projectKey=orabank \
                        -Dsonar.projectName=Orabank \
                        -Dsonar.sources=pro/src/main,e-bank-frontend/src \
                        -Dsonar.tests=pro/src/test \
                        -Dsonar.java.binaries=pro/target/classes \
                        -Dsonar.coverage.jacoco.xmlReportPaths=pro/target/site/jacoco/jacoco.xml \
                        -Dsonar.javascript.lcov.reportPaths=e-bank-frontend/coverage/lcov.info \
                        -Dsonar.exclusions=**/node_modules/**,**/*.test.*,**/.next/**
                    '''
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

        // 7. DEPLOY (main uniquement, sur la machine de deploiement)
        stage('Deploy') {
            when {
                allOf {
                    branch 'main'
                    expression { env.DEPLOY_ENABLED == 'true' }
                }
            }
            steps {
                echo 'Deploiement de Orabank'
                sh 'docker compose up -d --remove-orphans'
            }
        }

        // 8. SMOKE TEST
        stage('Smoke Test') {
            when {
                allOf {
                    branch 'main'
                    expression { env.DEPLOY_ENABLED == 'true' }
                }
            }
            steps {
                sh '''
                    check() {
                      for i in $(seq 1 24); do
                        if curl -sf "$1" > /dev/null; then echo "OK $1"; return 0; fi
                        echo "Attente $1 ($i/24)"; sleep 5
                      done
                      echo "ECHEC $1"; return 1
                    }
                    check http://localhost:PORT_BACKEND/actuator/health
                    check http://localhost:PORT_FRONTEND
                    check http://localhost:PORT_PROMETHEUS/-/healthy
                    check http://localhost:PORT_GRAFANA/api/health
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
