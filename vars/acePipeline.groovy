def call() {

    pipeline {
        agent any

        environment {
            ACE_HOME = "C:\\Program Files\\IBM\\ACE\\12.0.8.0"
            WORK_DIR = "E:\\ACE\\IS\\PRUEBAS_LOCAL"
        }

        stages {

            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            stage('Detect App Name') {
                steps {
                    script {
                        def projectFile = readFile '.project'
                        def matcher = (projectFile =~ /<name>(.*?)<\/name>/)

                        if (!matcher) {
                            error("No se encontró nombre de app en .project")
                        }

                        env.APP_NAME = matcher[0][1]

                        echo "App detectada: ${env.APP_NAME}"
                    }
                }
            }

            stage('Build BAR') {
                steps {
                    bat """
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

                    ibmint package ^
                        --input-path . ^
                        --output-bar-file %APP_NAME%.bar
                    """
                }
            }

            stage('Deploy') {
                steps {
                    bat """
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

                    ibmint deploy ^
                        --input-bar-file %APP_NAME%.bar ^
                        --output-work-directory %WORK_DIR%
                    """
                }
            }
        }
    }
}