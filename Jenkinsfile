pipeline {
    agent any

    tools {
        maven 'maven3'
    }

    environment {
        SCANNER_HOME = tool 'sonar-scanner'
        DOCKER_IMAGE = "vlad7774/borod-bank"
        // Tag includes build number + short git SHA for full traceability
        DOCKER_TAG   = "v${BUILD_NUMBER}-${GIT_COMMIT[0..7]}"
    }

    options {
        // Abort the whole pipeline if it runs longer than 60 minutes
        timeout(time: 60, unit: 'MINUTES')
        // Keep only last 10 builds
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // Don't run concurrent builds on the same branch
        disableConcurrentBuilds()
        // Add timestamps to console output
        timestamps()
    }

    stages {

        stage('Git Checkout') {
            steps {
                timeout(time: 2, unit: 'MINUTES') {
                    git branch: 'main', credentialsId: 'git-cred', url: 'https://github.com/Vlad774/borod-bank-ci.git'
                }
            }
        }

        // ── PARALLEL: Security & Lint — run together to save time ──────
        stage('Security & Lint') {
            parallel {

                stage('Gitleaks') {
                    steps {
                        timeout(time: 5, unit: 'MINUTES') {
                            sh '''
                                docker run --rm \
                                  -v $(pwd):/path \
                                  zricethezav/gitleaks:latest detect \
                                  --source /path \
                                  --report-format json \
                                  --report-path /path/gitleaks-report.json \
                                  --exit-code 1 \
                                  || (echo "Gitleaks found secrets!" && exit 1)
                            '''
                        }
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: 'gitleaks-report.json',
                                             allowEmptyArchive: true
                        }
                    }
                }

                stage('Hadolint') {
                    steps {
                        timeout(time: 3, unit: 'MINUTES') {
                            sh '''
                                docker run --rm \
                                  -i hadolint/hadolint:latest < Dockerfile \
                                  | tee hadolint-report.txt
                            '''
                        }
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: 'hadolint-report.txt',
                                             allowEmptyArchive: true
                        }
                    }
                }

            }
        }

        stage('Compile') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    sh 'mvn compile -q'
                }
            }
        }

        // ── PARALLEL: Tests & SAST — all independent, run together ─────
        stage('Tests & SAST') {
            parallel {

                stage('Unit Tests + Coverage') {
                    steps {
                        timeout(time: 10, unit: 'MINUTES') {
                            // JaCoCo agent is attached automatically via pom.xml
                            // jacoco:check will fail build if coverage < 70%
                            sh 'mvn test -q'
                        }
                    }
                    post {
                        always {
                            junit '**/target/surefire-reports/*.xml'
                            // Publish JaCoCo coverage report in Jenkins UI
                            jacoco(
                                execPattern: '**/target/jacoco.exec',
                                classPattern: '**/target/classes',
                                sourcePattern: '**/src/main/java',
                                minimumLineCoverage: '70'
                            )
                        }
                    }
                }

                stage('Trivy FS Scan') {
                    steps {
                        timeout(time: 10, unit: 'MINUTES') {
                            // Fail on CRITICAL with available fix — actionable findings only
                            sh '''
                                trivy fs \
                                  --format table \
                                  --exit-code 1 \
                                  --severity CRITICAL \
                                  --ignore-unfixed \
                                  -o trivy-fs-report.html \
                                  .
                            '''
                        }
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: 'trivy-fs-report.html',
                                             allowEmptyArchive: true
                        }
                    }
                }

            }
        }

        // ── SonarQube gets coverage data from JaCoCo XML report ─────────
        stage('SonarQube Analysis') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    withSonarQubeEnv('sonar') {
                        sh '''
                            ${SCANNER_HOME}/bin/sonar-scanner \
                              -Dsonar.projectName=BorodBank \
                              -Dsonar.projectKey=BorodBank \
                              -Dsonar.java.binaries=target/classes \
                              -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                              -Dsonar.junit.reportPaths=target/surefire-reports
                        '''
                    }
                }
            }
        }

        // ── Quality Gate — fail fast if SonarQube analysis didn't pass ──────
        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Build Package') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    sh 'mvn package -DskipTests -q'
                }
            }
        }

        stage('Docker Build & Tag') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    script {
                        withDockerRegistry(credentialsId: 'docker-cred') {
                            // --cache-from speeds up repeated builds
                            sh """
                                docker build \
                                  --cache-from ${DOCKER_IMAGE}:latest \
                                  -t ${DOCKER_IMAGE}:${DOCKER_TAG} \
                                  -t ${DOCKER_IMAGE}:latest \
                                  .
                            """
                        }
                    }
                }
            }
        }

        // ── PARALLEL: Image checks ───────────────────────────────────────
        stage('Image Checks') {
            parallel {

                stage('Trivy Image Scan') {
                    steps {
                        timeout(time: 10, unit: 'MINUTES') {
                            // Fail on CRITICAL/HIGH with available fix
                            sh """
                                trivy image \
                                  --format table \
                                  --exit-code 1 \
                                  --severity CRITICAL,HIGH \
                                  --ignore-unfixed \
                                  -o trivy-image-report.html \
                                  ${DOCKER_IMAGE}:${DOCKER_TAG}
                            """
                        }
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: 'trivy-image-report.html',
                                             allowEmptyArchive: true
                        }
                    }
                }

                stage('Checkov IaC Scan') {
                    steps {
                        timeout(time: 5, unit: 'MINUTES') {
                            sh '''
                                docker run --rm \
                                  -v $(pwd):/tf \
                                  bridgecrew/checkov:latest \
                                  --directory /tf \
                                  --framework dockerfile \
                                  --output cli \
                                  --soft-fail
                            '''
                        }
                    }
                }

            }
        }

        // ── Retry push up to 3 times on network errors ──────────────────
        stage('Docker Push') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    retry(3) {
                        script {
                            withDockerRegistry(credentialsId: 'docker-cred') {
                                sh "docker push ${DOCKER_IMAGE}:${DOCKER_TAG}"
                                sh "docker push ${DOCKER_IMAGE}:latest"
                            }
                        }
                    }
                }
            }
        }

        // ── Retry git push up to 3 times (race conditions on main) ──────
        stage('Update K8s Manifests') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    retry(3) {
                        withCredentials([usernamePassword(credentialsId: 'git-cred',
                                                          usernameVariable: 'GIT_USER',
                                                          passwordVariable: 'GIT_TOKEN')]) {
                            sh """
                                rm -rf borod-bank-cd
                                git clone https://\${GIT_USER}:\${GIT_TOKEN}@github.com/Vlad774/borod-bank-cd.git
                                cd borod-bank-cd/app
                                kustomize edit set image ${DOCKER_IMAGE}=${DOCKER_IMAGE}:${DOCKER_TAG}
                                git config user.email "jenkins@borod-bank.ci"
                                git config user.name "Jenkins CI"
                                git add kustomization.yaml
                                git commit -m "ci: update app image to ${DOCKER_TAG}"
                                git push https://\${GIT_USER}:\${GIT_TOKEN}@github.com/Vlad774/borod-bank-cd.git main
                            """
                        }
                    }
                }
            }
        }

    }

    post {
        always {
            deleteDir()
        }
        success {
            echo "Pipeline PASSED — ${DOCKER_IMAGE}:${DOCKER_TAG} is live"
        }
        failure {
            echo "Pipeline FAILED — check reports in Jenkins artifacts"
        }
        unstable {
            echo "Pipeline UNSTABLE — some tests may have failed"
        }
    }
}
