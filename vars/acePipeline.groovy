def call() {

    pipeline {
        agent any

        environment {
            ACE_HOME = "C:\\Program Files\\IBM\\ACE\\12.0.8.0"
            WORK_DIR = "E:\\ACE\\IS\\PRUEBAS_LOCAL"
            IS_NAME  = "server1"
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

            stage('Detect App (SAFE)') {
                steps {
                    script {

                        def projectFile = findFiles(glob: '**/.project')[0].path
                        echo "Project encontrado: ${projectFile}"

                        def projectContent = readFile(projectFile)
                        def matcher = projectContent =~ /<name>(.*?)<\/name>/

                        if (!matcher) {
                            error "No se pudo leer el nombre de la app desde .project"
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

            stage('Deploy to Integration Server (REAL)') {
				steps {
					bat """
					call "%ACE_HOME%\\server\\bin\\mqsiprofile.cmd"

					echo Deploying BAR to PRUEBAS_LOCAL...

					if not exist "%WORK_DIR%\\server.conf.yaml" (
						echo ERROR: PRUEBAS_LOCAL no es un Integration Server valido
						exit /b 1
					)

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

                    echo Restarting Integration Server...

                    mqsistop %IS_NAME%
                    timeout /t 3 /nobreak
                    mqsistart %IS_NAME%
                    """
                }
            }
        }

        post {
            success {
                echo "Deployment SUCCESS 🚀 App: ${env.APP_NAME}"
            }
            failure {
                echo "Deployment FAILED ❌"
            }
        }
    }
}