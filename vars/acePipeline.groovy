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
                            error "No .project found"
                        }

                        def content = readFile(projectFile[0].path)
                        def matcher = (content =~ /<name>(.*?)<\/name>/)

                        if (!matcher) {
                            error "Cannot read app name"
                        }

                        env.APP_NAME = matcher[0][1]
                        env.APP_ROOT = pwd()

                        echo "✔ App: ${env.APP_NAME}"
                    }
                }
            }

            stage('Build BAR') {
                steps {
                    bat """
                    call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

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

                    echo Restart not using mqsirestart (fix required)
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