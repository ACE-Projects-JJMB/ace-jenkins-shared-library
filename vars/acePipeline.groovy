def call(Map config = [:]) {

    pipeline {

        agent any

        environment {
            ACE_HOME = config.ACE_HOME ?: "C:\\Program Files\\IBM\\ACE\\12.0.8.0"
            IS_NAME  = config.IS_NAME  ?: "PRUEBAS_LOCAL"
            WORK_DIR = config.WORK_DIR ?: "E:\\ACE\\IS\\PRUEBAS_LOCAL"
        }

        stages {

            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            stage('Detect App') {
                steps {
                    script {

                        def projectFile = findFiles(glob: "**/.project")

                        if (projectFile.length == 0) {
                            error "No se encontró .project en el workspace"
                        }

                        def projectPath = projectFile[0].path
                        def projectContent = readFile(projectPath)

                        def matcher = (projectContent =~ /<name>(.*?)<\/name>/)

                        if (!matcher) {
                            error "No se pudo leer el nombre de la app en .project"
                        }

                        env.APP_NAME = matcher[0][1]
                        env.APP_ROOT = pwd()

                        echo "App detectada: ${env.APP_NAME}"
                        echo "Root: ${env.APP_ROOT}"
                    }
                }
            }

            stage('Build BAR') {
                steps {
                    bat """
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

                    echo Building BAR for %APP_NAME%...

                    ibmint package ^
                        --input-path "%APP_ROOT%" ^
                        --output-bar-file "%APP_NAME%.bar"
                    """
                }
            }

            stage('Deploy to Integration Server') {
                steps {
                    bat """
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

                    echo Deploying BAR to %IS_NAME%...

                    ibmint deploy ^
                        --input-bar-file "%APP_NAME%.bar" ^
                        --output-work-directory "%WORK_DIR%"
                    """
                }
            }

            stage('Restart Integration Server') {
                steps {
                    bat """
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

                    echo Restarting Integration Server %IS_NAME%...

                    mqsirestart %IS_NAME%
                    """
                }
            }
        }

        post {
            success {
                echo "Deployment SUCCESS ✅"
            }

            failure {
                echo "Deployment FAILED ❌"
            }
        }
    }
}