def call() {

    pipeline {
        agent any

        environment {
            ACE_HOME = "C:\\Program Files\\IBM\\ACE\\12.0.8.0"
            IS_NAME  = "PRUEBAS_LOCAL"
            WORK_DIR = "E:\\ACE\\IS\\PRUEBAS_LOCAL"
        }

        stages {

            stage('Clean Workspace') {
                steps {
                    deleteDir()
                }
            }

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
                            error "No se encontró .project"
                        }

                        def projectContent = readFile(projectFile[0].path)
                        def matcher = (projectContent =~ /<name>(.*?)<\/name>/)

                        if (!matcher) {
                            error "No se pudo leer el nombre de la app"
                        }

                        env.APP_NAME = matcher[0][1]
                        env.APP_ROOT = pwd()

                        echo "✔ App detectada: ${env.APP_NAME}"
                        echo "✔ Root: ${env.APP_ROOT}"
                    }
                }
            }

            stage('Build BAR') {
                steps {
                    bat """
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

                    echo Building BAR...

                    ibmint package ^
                      --input-path "%APP_ROOT%" ^
                      --output-bar-file "%APP_NAME%.bar"
                    """
                }
            }

            stage('Deploy') {
                steps {
                    bat """
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

                    echo Deploying to %IS_NAME%...

                    mqsibar -a "%APP_NAME%.bar" -c %IS_NAME%
                    """
                }
            }

            stage('Restart Integration Server') {
                steps {
                    bat """
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

                    echo Restarting %IS_NAME%...

                    mqsistop %IS_NAME%
                    timeout /t 5
                    mqsistart %IS_NAME%
                    """
                }
            }
        }

        post {
            success {
                echo "✔ Deployment OK"
            }
            failure {
                echo "❌ Deployment FAILED"
            }
        }
    }
}