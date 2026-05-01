def call(Map config = [:]) {

    // ===== CONFIG SEGURA (SIN environment block problemático) =====
    def ACE_HOME = "C:\\Program Files\\IBM\\ACE\\12.0.8.0"
    def IS_NAME  = config.IS_NAME ?: "PRUEBAS_LOCAL"
    def WORK_DIR = config.WORK_DIR ?: "E:\\ACE\\IS\\PRUEBAS_LOCAL"

    pipeline {
        agent any

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

                        if (!projectFile || projectFile.length == 0) {
                            error "❌ No se encontró archivo .project"
                        }

                        def content = readFile(projectFile[0].path)

                        def matcher = (content =~ /<name>(.*?)<\/name>/)

                        if (!matcher || matcher.size() == 0) {
                            error "❌ No se pudo leer el nombre de la aplicación"
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
                    call "${ACE_HOME}\\server\\bin\\mqsiprofile.cmd"

                    echo Building BAR...

                    ibmint package ^
                      --input-path "%APP_ROOT%" ^
                      --output-bar-file "%APP_NAME%.bar"
                    """
                }
            }

            stage('Deploy to Integration Server') {
                steps {
                    bat """
                    call "${ACE_HOME}\\server\\bin\\mqsiprofile.cmd"

                    echo Deploying to %IS_NAME%...

                    ibmint deploy ^
                      --input-bar-file "%APP_NAME%.bar" ^
                      --output-work-directory "%WORK_DIR%"
                    """
                }
            }

            stage('Restart Integration Server') {
                steps {
                    bat """
                    call "${ACE_HOME}\\server\\bin\\mqsiprofile.cmd"

                    echo Restarting %IS_NAME%...

                    mqsirestart %IS_NAME%
                    """
                }
            }
        }

        post {
            success {
                echo "✅ Deployment OK"
            }

            failure {
                echo "❌ Deployment FAILED"
            }
        }
    }
}