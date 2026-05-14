def call(Map config = [:]) {

    def appName = config.appName
    def aceHome = config.aceHome ?: 'C:\\Program Files\\IBM\\ACE\\12.0.8.0'

    pipeline {

        agent any

        environment {
            ACE_HOME = "${aceHome}"
            APP_NAME = "${appName}"
        }

        stages {

            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            stage('Build') {
                steps {
                    bat """
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

                    echo Building ALL (APP + LIB if exists)

                    ibmint package ^
                      --input-path "%WORKSPACE%" ^
                      --output-bar-file "%APP_NAME%.bar"
                    """
                }
            }

            stage('Deploy') {
                steps {
                    bat """
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

                    echo Deploying APP

                    mqsideploy ^
                      -i localhost ^
                      -p 7600 ^
                      -a "%APP_NAME%.bar"
                    """
                }
            }
        }

        post {
            success {
                echo "Deployment SUCCESS for ${APP_NAME}"
            }
            failure {
                echo "Deployment FAILED for ${APP_NAME}"
            }
        }
    }
}