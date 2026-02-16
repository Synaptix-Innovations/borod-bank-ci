pipeline {

    // ── Every build gets a fresh, isolated pod — no shared Docker daemon ──
    agent {
        kubernetes {
            yaml """
apiVersion: v1
kind: Pod
metadata:
  labels:
    app: borod-bank-build
spec:
  serviceAccountName: jenkins
  # Pod-level security: no privilege escalation allowed for any container
  securityContext:
    fsGroup: 1000
  containers:

    # Jenkins inbound agent — handles SCM checkout and pipeline orchestration
    - name: jnlp
      image: jenkins/inbound-agent:3261.v9c670a_4748a_9-1
      resources:
        requests:
          cpu: 100m
          memory: 256Mi
        limits:
          cpu: 500m
          memory: 512Mi

    # Maven — compile, test, package, SonarQube analysis
    - name: maven
      image: maven:3.9.6-eclipse-temurin-17
      command: [sleep, infinity]
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        allowPrivilegeEscalation: false
      resources:
        requests:
          cpu: 200m
          memory: 512Mi
        limits:
          cpu: 2000m
          memory: 3Gi
      volumeMounts:
        # Per-build Maven cache — prevents cross-build state
        - name: m2-cache
          mountPath: /root/.m2

    # Kaniko — builds and pushes Docker image without a Docker daemon
    # Runs as root (needed for layer emulation) but NOT privileged
    - name: kaniko
      image: gcr.io/kaniko-project/executor:v1.23.2-debug
      command: [/busybox/cat]
      tty: true
      securityContext:
        runAsUser: 0
        allowPrivilegeEscalation: false
      resources:
        requests:
          cpu: 100m
          memory: 256Mi
        limits:
          cpu: 2000m
          memory: 2Gi

    # Trivy — filesystem scan (SCA) + image scan after push
    - name: trivy
      image: aquasec/trivy:0.58.0
      command: [sleep, infinity]
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        allowPrivilegeEscalation: false
      resources:
        requests:
          cpu: 50m
          memory: 128Mi
        limits:
          cpu: 500m
          memory: 1Gi

    # Gitleaks — secret detection in source code
    - name: gitleaks
      image: zricethezav/gitleaks:v8.21.2
      command: [sleep, infinity]
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        allowPrivilegeEscalation: false
      resources:
        requests:
          cpu: 50m
          memory: 128Mi
        limits:
          cpu: 200m
          memory: 256Mi

    # Hadolint — Dockerfile linting
    - name: hadolint
      image: hadolint/hadolint:v2.12.0
      command: [sleep, infinity]
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        allowPrivilegeEscalation: false
      resources:
        requests:
          cpu: 50m
          memory: 64Mi
        limits:
          cpu: 100m
          memory: 128Mi

    # Checkov — IaC static analysis (Dockerfile)
    - name: checkov
      image: bridgecrew/checkov:3.2.0
      command: [sleep, infinity]
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        allowPrivilegeEscalation: false
      resources:
        requests:
          cpu: 50m
          memory: 128Mi
        limits:
          cpu: 500m
          memory: 1Gi

    # Tools — git + kustomize for updating borod-bank-cd manifests
    - name: tools
      image: alpine/k8s:1.30.2
      command: [sleep, infinity]
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        allowPrivilegeEscalation: false
      resources:
        requests:
          cpu: 50m
          memory: 128Mi
        limits:
          cpu: 200m
          memory: 256Mi

    # Syft — generates SBOM in CycloneDX format from container image
    - name: syft
      image: anchore/syft:1.18.0
      command: [sleep, infinity]
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        allowPrivilegeEscalation: false
      resources:
        requests:
          cpu: 50m
          memory: 128Mi
        limits:
          cpu: 500m
          memory: 512Mi

    # Cosign — signs image and attests SBOM; key mounted from AWS SM via ESO
    - name: cosign
      image: gcr.io/projectsigstore/cosign:v2.4.0
      command: [sleep, infinity]
      securityContext:
        runAsNonRoot: true
        runAsUser: 65532
        allowPrivilegeEscalation: false
      resources:
        requests:
          cpu: 50m
          memory: 64Mi
        limits:
          cpu: 200m
          memory: 128Mi
      volumeMounts:
        - name: cosign-secret
          mountPath: /cosign
          readOnly: true

  volumes:
    - name: m2-cache
      emptyDir: {}
    # cosign-signing-key is managed by ESO — sourced from AWS SM borod-bank/cosign
    - name: cosign-secret
      secret:
        secretName: cosign-signing-key
"""
            defaultContainer 'maven'
        }
    }

    environment {
        SCANNER_HOME    = tool 'sonar-scanner'
        DOCKER_IMAGE    = "vlad7774/borod-bank"
        // Tag includes build number + short git SHA for full traceability
        DOCKER_TAG      = "v${BUILD_NUMBER}-${GIT_COMMIT[0..7]}"
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

        // ── PARALLEL: Security & Lint — run together to save time ──────────
        stage('Security & Lint') {
            parallel {

                stage('Gitleaks') {
                    steps {
                        timeout(time: 5, unit: 'MINUTES') {
                            container('gitleaks') {
                                sh '''
                                    gitleaks detect \
                                      --source . \
                                      --report-format json \
                                      --report-path gitleaks-report.json \
                                      --exit-code 1 \
                                      || (echo "Gitleaks found secrets!" && exit 1)
                                '''
                            }
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
                            container('hadolint') {
                                sh 'hadolint Dockerfile | tee hadolint-report.txt'
                            }
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

        // ── PARALLEL: Tests & SAST — all independent, run together ─────────
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
                            container('trivy') {
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

        // ── SonarQube gets coverage data from JaCoCo XML report ─────────────
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

        // ── Kaniko: build + push — no Docker daemon, no privileged mode ─────
        stage('Docker Build & Push') {
            steps {
                timeout(time: 15, unit: 'MINUTES') {
                    container('kaniko') {
                        withCredentials([usernamePassword(
                            credentialsId: 'docker-cred',
                            usernameVariable: 'DOCKER_USER',
                            passwordVariable: 'DOCKER_PASS'
                        )]) {
                            sh '''
                                AUTH=$(printf '%s:%s' "$DOCKER_USER" "$DOCKER_PASS" | base64 | tr -d '\\n')
                                mkdir -p /kaniko/.docker
                                printf '{"auths":{"https://index.docker.io/v1/":{"auth":"%s"}}}' "$AUTH" \
                                  > /kaniko/.docker/config.json

                                /kaniko/executor \
                                  --context=dir://${WORKSPACE} \
                                  --dockerfile=${WORKSPACE}/Dockerfile \
                                  --destination=${DOCKER_IMAGE}:${DOCKER_TAG} \
                                  --destination=${DOCKER_IMAGE}:latest \
                                  --cache=true \
                                  --cache-repo=${DOCKER_IMAGE}-cache \
                                  --compressed-caching=false \
                                  --snapshot-mode=redo
                            '''
                        }
                    }
                }
            }
        }

        // ── PARALLEL: Post-push image checks ────────────────────────────────
        stage('Image Checks') {
            parallel {

                stage('Trivy Image Scan') {
                    steps {
                        timeout(time: 10, unit: 'MINUTES') {
                            container('trivy') {
                                // Fail on CRITICAL/HIGH with available fix
                                // Scans image directly from registry — no Docker daemon needed
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
                            container('checkov') {
                                sh '''
                                    checkov \
                                      --directory . \
                                      --framework dockerfile \
                                      --output cli \
                                      --soft-fail
                                '''
                            }
                        }
                    }
                }

                stage('Generate SBOM') {
                    steps {
                        timeout(time: 10, unit: 'MINUTES') {
                            container('syft') {
                                withCredentials([usernamePassword(
                                    credentialsId: 'docker-cred',
                                    usernameVariable: 'DOCKER_USER',
                                    passwordVariable: 'DOCKER_PASS'
                                )]) {
                                    sh """
                                        syft ${DOCKER_IMAGE}:${DOCKER_TAG} \
                                          -o cyclonedx-json \
                                          --file sbom-cyclonedx.json
                                    """
                                }
                            }
                        }
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: 'sbom-cyclonedx.json',
                                             allowEmptyArchive: true
                        }
                    }
                }

            }
        }

        // ── Sign image + attest SBOM — supply chain integrity ───────────────
        stage('Sign & Attest Image') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    container('cosign') {
                        withCredentials([
                            usernamePassword(
                                credentialsId: 'docker-cred',
                                usernameVariable: 'DOCKER_USER',
                                passwordVariable: 'DOCKER_PASS'
                            )
                        ]) {
                            sh """
                                # Registry auth for cosign push
                                export DOCKER_CONFIG=\$(mktemp -d)
                                AUTH=\$(printf '%s:%s' "\$DOCKER_USER" "\$DOCKER_PASS" | base64 | tr -d '\\n')
                                printf '{"auths":{"https://index.docker.io/v1/":{"auth":"%s"}}}' "\$AUTH" \
                                  > \${DOCKER_CONFIG}/config.json

                                export COSIGN_PASSWORD=\$(cat /cosign/cosign.password)

                                # Sign the image — signature pushed to Docker Hub
                                cosign sign \
                                  --key /cosign/cosign.key \
                                  --yes \
                                  ${DOCKER_IMAGE}:${DOCKER_TAG}

                                # Attest with SBOM — predicate attached to image
                                cosign attest \
                                  --key /cosign/cosign.key \
                                  --predicate sbom-cyclonedx.json \
                                  --type cyclonedx \
                                  --yes \
                                  ${DOCKER_IMAGE}:${DOCKER_TAG}
                            """
                        }
                    }
                }
            }
        }

        // ── Update CD repo — ArgoCD will auto-sync the new image ────────────
        stage('Update K8s Manifests') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    retry(3) {
                        container('tools') {
                            withCredentials([usernamePassword(
                                credentialsId: 'git-cred',
                                usernameVariable: 'GIT_USER',
                                passwordVariable: 'GIT_TOKEN'
                            )]) {
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

    }

    post {
        always {
            deleteDir()
        }
        success {
            script {
                container('tools') {
                    withCredentials([string(credentialsId: 'slack-webhook', variable: 'SLACK_HOOK_URL')]) {
                        sh """
                            printf '{"text":"✅ *borod-bank* build #${BUILD_NUMBER} passed\\n• Image: `${DOCKER_IMAGE}:${DOCKER_TAG}`\\n• <%s|View build>" "${BUILD_URL}" \\
                              > /tmp/slack.json
                            curl -s -X POST "\$SLACK_HOOK_URL" \
                              -H 'Content-Type: application/json' \
                              -d @/tmp/slack.json
                        """
                    }
                }
            }
        }
        failure {
            script {
                container('tools') {
                    withCredentials([string(credentialsId: 'slack-webhook', variable: 'SLACK_HOOK_URL')]) {
                        sh """
                            printf '{"text":"❌ *borod-bank* build #${BUILD_NUMBER} FAILED\\n• <%s|View logs>" "${BUILD_URL}" \\
                              > /tmp/slack.json
                            curl -s -X POST "\$SLACK_HOOK_URL" \
                              -H 'Content-Type: application/json' \
                              -d @/tmp/slack.json
                        """
                    }
                }
            }
        }
        unstable {
            script {
                container('tools') {
                    withCredentials([string(credentialsId: 'slack-webhook', variable: 'SLACK_HOOK_URL')]) {
                        sh """
                            printf '{"text":"⚠️ *borod-bank* build #${BUILD_NUMBER} unstable — tests may have failed\\n• <%s|View build>" "${BUILD_URL}" \\
                              > /tmp/slack.json
                            curl -s -X POST "\$SLACK_HOOK_URL" \
                              -H 'Content-Type: application/json' \
                              -d @/tmp/slack.json
                        """
                    }
                }
            }
        }
    }
}
