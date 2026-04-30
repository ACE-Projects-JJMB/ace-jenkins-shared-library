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

            stage('Detect App Name') {
				steps {
					script {
						def repoUrl = scm.getUserRemoteConfigs()[0].getUrl()
						def appName = repoUrl.tokenize('/').last().replace('.git','')

						env.APP_NAME = appName

						echo "App detectada (desde repo): ${env.APP_NAME}"
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