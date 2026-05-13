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

            stage('Detect App Folder') {
                steps {
                    script {

                        def folder = bat(
                            script: """
                            @echo off
                            for /d %%i in ("%WORKSPACE%\\app\\*") do (
                                if exist "%%i\\application.descriptor" (
                                    echo %%~nxi
                                    goto :end
                                )
                            )
                            :end
                            """,
                            returnStdout: true
                        ).trim()

                        env.APP_FOLDER = folder

                        echo "Detected Application Folder: ${env.APP_FOLDER}"
                    }
                }
            }

            stage('Build BAR') {
                steps {
                    bat """
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

                    echo Building BAR for %APP_NAME%

                    ibmint package ^
                      --input-path "%WORKSPACE%\\app\\%APP_FOLDER%" ^
                      --output-bar-file "%APP_NAME%.bar"
                    """
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
                echo "Deployment SUCCESS for ${APP_NAME}"
            }
            failure {
                echo "Deployment FAILED for ${APP_NAME}"
            }
        }
    }
}