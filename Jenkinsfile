// CI/CD for a service built from ipie-service-template.
//
// A service generated from the template inherits this file. Change the image name in `environment`
// and the rest applies unchanged - the point is that every service is verified the same way, so a
// reviewer can trust "the pipeline is green" to mean the same thing everywhere.
//
// The gate is `./gradlew check`, which is what a developer runs locally: Checkstyle, SpotBugs,
// ArchUnit, unit and integration tests. Pointing CI at that one task is deliberate - a separate
// list of checks here would drift from the local one, and the drift would only be discovered when
// something got through.
//
// AGENT REQUIREMENTS
//   * JDK 21 - matches JavaLanguageVersion.of(21) in ipie.java-conventions
//   * A working Docker socket - the integration tests use Testcontainers and start a real
//     PostgreSQL. Without it every context load fails on a connection error that looks like a
//     configuration fault and is not one.
//
// JENKINS CONFIGURATION EXPECTED
//   Tool         'jdk-21'                     JDK installation
//   Credential   'ipie-github-packages'       username/password - resolves the platform artifacts
//   Credential   'ipie-container-registry'    username/password - pushes images
//   Server       'ipie-sonarqube'             SonarQube server, once SONAR_ENABLED is true
//   Plugins      Pipeline, Git/GitHub Branch Source, Credentials Binding, JUnit, Coverage,
//                Warnings NG, Docker Pipeline, SonarQube Scanner
//   Env          IPIE_REGISTRY                registry host, e.g. registry.example.gov.in/ipie

pipeline {
    agent { label 'docker' }

    options {
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
        disableConcurrentBuilds(abortPrevious: true)
        timestamps()
    }

    tools {
        jdk 'jdk-21'
    }

    environment {
        // Rename this when the service is created from the template. It is the only line that has
        // to change.
        SERVICE_NAME = 'ipie-service-template'

        // The build resolves ipie-parent and ipie-common-libs from GitHub Packages. settings.gradle
        // falls back to these variable names when the gradle properties are absent, so binding the
        // credential to them is enough - no properties file on the agent.
        PACKAGES = credentials('ipie-github-packages')
        GITHUB_ACTOR = "${PACKAGES_USR}"
        GITHUB_TOKEN = "${PACKAGES_PSW}"

        // Keep Gradle's home inside the workspace so a build cannot inherit state from another job,
        // and off the daemon so an agent is not left holding JVMs between builds.
        GRADLE_USER_HOME = "${WORKSPACE}/.gradle"
        GRADLE_OPTS = '-Dorg.gradle.daemon=false -Dorg.gradle.console=plain'

        IMAGE = "${env.IPIE_REGISTRY}/${SERVICE_NAME}:${GIT_COMMIT}"

        // 'false' until the SonarQube Gradle plugin is added to the conventions and a server exists
        // - see the SonarQube stage. The pipeline is usable before either of those is true.
        SONAR_ENABLED = 'false'
    }

    stages {
        stage('Build') {
            steps {
                sh './gradlew --no-daemon classes'
            }
        }

        stage('Check') {
            // Checkstyle, SpotBugs, ArchUnit, unit and integration tests. Reports are published in
            // post{} rather than here, so a failing gate still surfaces its findings.
            steps {
                sh './gradlew --no-daemon check'
            }
        }

        stage('SonarQube') {
            // PREREQUISITE: the `sonar` task does not exist yet. ipie.java-conventions applies
            // Checkstyle, SpotBugs and JaCoCo but not the SonarQube Gradle plugin, so this stage
            // must stay disabled (SONAR_ENABLED = 'false') until that plugin is added to the
            // conventions - one line there, and every service gets the task at once. Adding it here
            // instead would put a build concern in the pipeline and let the two drift.
            when {
                expression { env.SONAR_ENABLED == 'true' }
            }
            steps {
                withSonarQubeEnv('ipie-sonarqube') {
                    sh '''
                        ./gradlew --no-daemon sonar \
                          -Dsonar.projectKey=${SERVICE_NAME} \
                          -Dsonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/test/jacocoTestReport.xml \
                          -Dsonar.java.checkstyle.reportPaths=build/reports/checkstyle/main.xml
                    '''
                }
                // The gate is asked for its verdict rather than assumed: without this the pipeline
                // reports success while Sonar is still deciding, which is the failure mode that
                // makes a quality gate decorative.
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Package') {
            steps {
                sh './gradlew --no-daemon bootJar'
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            }
        }

        stage('Image') {
            // Only the long-lived branches produce an image. A pull request is verified, not
            // published - an image built from a branch that may never merge is one more thing in
            // the registry that nobody can account for.
            when {
                anyOf {
                    branch 'develop'; branch 'test'; branch 'uat'; branch 'preprod'; branch 'master'
                }
            }
            steps {
                script {
                    docker.withRegistry("https://${env.IPIE_REGISTRY}", 'ipie-container-registry') {
                        // Tagged by commit SHA and never by branch or 'latest'. The digest is what
                        // gets promoted between environments, so the binary validated in UAT is
                        // provably the binary that reaches Production.
                        def image = docker.build("${IMAGE}")
                        image.push()
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                anyOf {
                    branch 'develop'; branch 'test'; branch 'uat'; branch 'preprod'; branch 'master'
                }
            }
            steps {
                script {
                    // Branch to environment to Spring profile, per Development Environment
                    // Configuration Section 45. One table, so the three cannot disagree.
                    //
                    // The profile string is 'deployed,<env>' and the order is load-bearing: Spring
                    // applies profile documents in the order listed, so the environment profile
                    // must come second to override the shared hardening in application-deployed.yml.
                    // Setting only '<env>' would start the service on application.yml's
                    // development-friendly defaults with none of that hardening.
                    def mapping = [
                        develop: ['DEV',  'deployed,dev'],
                        test   : ['SIT',  'deployed,sit'],
                        uat    : ['UAT',  'deployed,uat'],
                        preprod: ['PPE',  'deployed,preprod'],
                        master : ['PROD', 'deployed,prod'],
                    ][env.BRANCH_NAME]
                    def target = mapping[0]
                    def springProfiles = mapping[1]

                    // Production is not deployed by a branch merge alone. The change record is the
                    // control that says a human decided this release goes now, and the pipeline
                    // should not be the thing that quietly bypasses it.
                    if (target == 'PROD') {
                        timeout(time: 60, unit: 'MINUTES') {
                            input message: "Deploy ${SERVICE_NAME} ${GIT_COMMIT} to PROD?",
                                  submitter: 'release-managers'
                        }
                    }

                    echo "Deploying ${IMAGE} to ${target} with SPRING_PROFILES_ACTIVE=${springProfiles}"

                    // DELIBERATELY NOT IMPLEMENTED. The container orchestrator and its endpoints
                    // are still to be confirmed (Development Environment Configuration Section 43,
                    // where environment endpoints are marked TBC). Wiring this to a guess would
                    // produce a pipeline that looks complete and deploys nowhere. Replace this step
                    // with the orchestrator call once those are settled; the image reference and
                    // the target environment above are everything it needs.
                    error("Deploy step is not configured yet - see the comment in the Jenkinsfile")
                }
            }
        }
    }

    post {
        always {
            junit testResults: 'build/test-results/test/*.xml', allowEmptyResults: true

            recordCoverage tools: [[parser: 'JACOCO',
                                    pattern: 'build/reports/jacoco/test/jacocoTestReport.xml']]

            // Checkstyle publishes XML; SpotBugs does not. ipie.java-conventions sets
            // xml.required = false on spotbugsMain, so there is nothing here for Jenkins to parse -
            // the build still FAILS on a SpotBugs finding (ignoreFailures = false), so the gate
            // holds, but the finding will only be visible in the console and the HTML report.
            // Enabling XML output in the conventions would give Jenkins the trend as well.
            recordIssues enabledForFailure: true,
                         tools: [checkStyle(pattern: 'build/reports/checkstyle/*.xml')]
        }
        cleanup {
            cleanWs()
        }
    }
}
