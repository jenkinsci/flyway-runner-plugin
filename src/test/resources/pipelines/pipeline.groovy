pipeline {
    agent {
        label('test-agent')
    }
    stages {
        stage('Checkout') {
            steps {
                sh('mkdir -p migrations')
                writeFile(file: 'migrations/V1__init.sql', text: 'CREATE TABLE test (id INT);')
            }
        }
        stage('Run Flyway') {
            steps {
                flywayrunner(
                    flywayCommand: 'migrate',
                    installationName: 'flyway',
                    locations: "filesystem:${env.WORKSPACE}/migrations",
                    url: 'jdbc:h2:mem:test',
                    credentialsId: 'pipeline-credentials',
                    commandLineArgs: '',
                )
            }
        }
    }
}
