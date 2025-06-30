pipeline {
  agent any
  tools {
    maven 'Maven 3.8.5'
    jdk 'OpenJDK11'
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '5'))
    timestamps ()
  }
  parameters {
    booleanParam(name: 'RELEASE',
            defaultValue: false,
            description: 'Do a Maven release of the project')
  }
  stages {
    stage('Build') {
      when {
        not { expression { params.RELEASE } }
      }
      steps {
          withMaven(traceability: true) {
            configFileProvider(
                    [configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                            variable: 'MAVEN_SETTINGS_XML')]) {
              sh 'mvn clean package deploy dependency:analyze -U'
            }
          }
      }
    }
    stage('SonarQube analysis') {
      tools {
        jdk "OpenJDK11"
      }
      when {
        not { expression { params.RELEASE } }
      }
      steps {
          withSonarQubeEnv('GBIF Sonarqube') {
              withCredentials([usernamePassword(credentialsId: 'SONAR_CREDENTIALS', usernameVariable: 'SONAR_USER', passwordVariable: 'SONAR_PWD')]) {
                  sh 'mvn sonar:sonar -Dsonar.login=${SONAR_USER} -Dsonar.password=${SONAR_PWD} -Dsonar.server=${SONAR_HOST_URL}'
              }
          }
      }
    }
    stage('Release version to nexus') {
      when { expression { params.RELEASE } }
      steps {
        configFileProvider(
                [configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                        variable: 'MAVEN_SETTINGS_XML')]) {
          git 'https://github.com/gbif/collections-duplicates.git'
          sh 'mvn -s $MAVEN_SETTINGS_XML -B release:prepare release:perform'
        }
      }
    }
  }
}
