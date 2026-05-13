def call(Map config = [:]) {

    def appName = config.appName
    def workDir = config.workDir
    def isName  = config.isName
    def aceHome = config.aceHome ?: 'C:\\Program Files\\IBM\\ACE\\12.0.8.0'

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
                }
            }

            stage('Deploy') {
                steps {
                    bat """
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

                    echo Deploying %APP_NAME%

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
                echo "Deployment SUCCESS for ${appName}"
            }
            failure {
                echo "Deployment FAILED for ${appName}"
            }
        }
    }
}   // 👈 ESTA LLAVE ES LA QUE TE FALTABA (MUY PROBABLEMENTE)