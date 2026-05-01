def call() {

    pipeline {

        agent any

        environment {
            ACE_HOME = 'C:\\Program Files\\IBM\\ACE\\12.0.8.0'
            IS_NAME  = 'PRUEBAS_LOCAL'
            WORK_DIR = 'E:\\ACE\\IS\\PRUEBAS_LOCAL'
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

                        def content = readFile(projectFile[0].path)
                        def matcher = (content =~ /<name>(.*?)<\/name>/)

                        if (!matcher) {
                            error "No se pudo leer nombre de app"
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
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

                    echo Deploying to Integration Server %IS_NAME%

                    ibmint deploy ^
                      --input-bar-file "%APP_NAME%.bar" ^
                      --output-work-directory "%WORK_DIR%"

                    echo Deployment completed
                    """
                }
            }

            stage('Restart Integration Server') {
                steps {
                    bat """
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

                    echo Restarting Integration Server %IS_NAME%

                    mqsireload %IS_NAME%

                    echo Reload done
                    """
                }
            }

            stage('Verify Deployment') {
                steps {
                    bat """
                    echo Checking work directory...

                    dir "%WORK_DIR%"
                    """
                }
            }
        }

        post {
            success {
                echo "Deployment SUCCESS"
            }
            failure {
                echo "Deployment FAILED"
            }
        }
    }
}