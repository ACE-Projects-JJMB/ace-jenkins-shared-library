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

            stage('Detect App Name & Root (SAFE)') {
                steps {
                    script {

                        // ==================================================
                        // BUSCAR .project SIN returnStdout (modo seguro)
                        // ==================================================
                        bat '''
                        @echo off
                        if exist files.txt del files.txt

                        for /r %%i in (.project) do (
                            echo %%i >> files.txt
                        )
                        '''

                        def file = readFile('files.txt').trim()

                        if (!file || file.length() == 0) {
                            error "No se encontró .project (no es app ACE válida)"
                        }

                        def projectFiles = file.split("\\r?\\n")
                        def appProjectPath = projectFiles[0].trim()

                        echo "Project encontrado: ${appProjectPath}"

                        // ==================================================
                        // LEER NOMBRE DE APP
                        // ==================================================
                        def projectContent = readFile(appProjectPath)

                        def matcher = projectContent =~ /<name>(.*?)<\\/name>/

                        if (!matcher.find()) {
                            error "No se pudo leer nombre de app en .project"
                        }

                        env.APP_NAME = matcher.group(1)

                        // quitar .project de forma segura
                        env.APP_ROOT = appProjectPath.replace("\\.project", "")

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