def call() {

    pipeline {
        agent any

        environment {
            ACE_HOME = "C:\\Program Files\\IBM\\ACE\\12.0.8.0"
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

            stage('Detect App Name & Root') {
                steps {
                    script {

                        // Buscar .project correctamente
                        def projectFiles = bat(
                            script: 'dir /s /b .project',
                            returnStdout: true
                        ).trim().split("\\r?\\n")

                        if (projectFiles.size() == 0 || projectFiles[0].trim() == "") {
                            error "No se encontró .project (no es app ACE válida)"
                        }

                        def appProjectPath = projectFiles[0].trim()
                        def appRoot = appProjectPath.replace("\\.project", "")

                        // Leer contenido del .project
                        def projectContent = readFile(appProjectPath)

                        def matcher = projectContent =~ /<name>(.*?)<\\/name>/

                        if (!matcher.find()) {
                            error "No se pudo leer nombre de app en .project"
                        }

                        env.APP_NAME = matcher.group(1)
                        env.APP_ROOT = appRoot

                        echo "App ACE detectada: ${env.APP_NAME}"
                        echo "App Root detectado: ${env.APP_ROOT}"
                    }
                }
            }

            stage('Build BAR') {
                steps {
                    bat """
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

                    ibmint package ^
                        --input-path "%WORKSPACE%\\%APP_ROOT%" ^
                        --output-bar-file "%APP_NAME%.bar"
                    """
                }
            }

            stage('Deploy') {
                steps {
                    bat """
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

                    ibmint deploy ^
                        --input-bar-file "%APP_NAME%.bar" ^
                        --output-work-directory "%WORK_DIR%"
                    """
                }
            }
        }
    }
}