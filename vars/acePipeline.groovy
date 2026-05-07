def call(Map config = [:]) {

    def appName = config.appName
    def workDir = config.workDir
    def isName  = config.isName
    def aceHome = config.aceHome ?: 'C:\\Program Files\\IBM\\ACE\\12.0.8.0'

    if (!appName) {
        error "appName es obligatorio"
    }

    pipeline {

        agent any

        environment {
            ACE_HOME = "${aceHome}"
            APP_NAME = "${appName}"
            WORK_DIR = "${workDir}"
            IS_NAME  = "${isName}"
        }

        stages {

            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            stage('Build BAR') {
                steps {
                    bat """
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

                    echo Building BAR for %APP_NAME%

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

                    echo Deploying %APP_NAME% to Integration Server

                    ibmint deploy ^
                      --input-bar-file "%APP_NAME%.bar" ^
                      --output-work-directory "%WORK_DIR%"

                    echo Deployment completed
                    """
                }
            }

        }

        post {
            success {
                echo "Deployment SUCCESS for ${appName}"
            }
            failure {
                echo "Deployment FAILED for ${appName}"
            }
        }
    }
}