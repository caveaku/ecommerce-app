pipeline {
    agent any

    environment {
        APP_NAME    = 'ecommerce-app'
        IMAGE_NAME  = 'ecommerce-app'
        JAR_NAME    = 'ecommerce-app-1.0.0.jar'
        CONTAINER   = 'ecommerce'
        APP_PORT    = '8080'
    }

    tools {
        maven 'Maven3'
        jdk   'JDK17'
    }

    stages {

        stage('Checkout') {
            steps {
                echo '>>> Pulling source code from GitHub...'
                git branch: 'main',
                    url: 'https://github.com/caveaku/ecommerce-app.git'
            }
        }

        stage('Build') {
            steps {
                echo '>>> Compiling source code...'
                sh 'mvn clean compile -q'
            }
        }

        stage('Test') {
            steps {
                echo '>>> Running unit tests...'
                sh 'mvn test'
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml',
                          allowEmptyResults: true
                }
                failure {
                    echo '>>> Tests failed. Aborting pipeline.'
                }
            }
        }

        stage('Package') {
            steps {
                echo '>>> Packaging application into JAR...'
                sh 'mvn package -DskipTests -q'
                archiveArtifacts artifacts: "target/${JAR_NAME}",
                                 fingerprint: true
            }
        }

        stage('Docker Build') {
            steps {
                echo '>>> Building Docker image...'
                sh "docker build -t ${IMAGE_NAME}:${BUILD_NUMBER} ."
                sh "docker tag ${IMAGE_NAME}:${BUILD_NUMBER} ${IMAGE_NAME}:latest"
            }
        }

        stage('Deploy') {
            steps {
                echo '>>> Deploying container...'
                sh """
                    # Stop and remove old container if it exists
                    docker stop ${CONTAINER} 2>/dev/null || true
                    docker rm   ${CONTAINER} 2>/dev/null || true

                    # Run new container
                    docker run -d \
                        --name ${CONTAINER} \
                        --restart unless-stopped \
                        -p ${APP_PORT}:${APP_PORT} \
                        ${IMAGE_NAME}:latest
                """
            }
        }

        stage('Health Check') {
            steps {
                echo '>>> Waiting for application to start...'
                sh 'sleep 20'
                sh "curl -f http://localhost:${APP_PORT}/actuator/health"
            }
        }

    }

    post {
        success {
            echo """
            ================================================
             BUILD SUCCESSFUL
             App running at: http://<EC2-PUBLIC-IP>:${APP_PORT}
            ================================================
            """
        }
        failure {
            echo '>>> Pipeline failed. Check the logs above.'
        }
        always {
            echo '>>> Cleaning up old Docker images...'
            sh "docker image prune -f"
        }
    }
}
