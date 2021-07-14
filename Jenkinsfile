pipeline {
  agent any
  tools {
    maven 'Maven3.2'
    jdk 'OpenJDK8'
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
        configFileProvider(
                [configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                        variable: 'MAVEN_SETTINGS_XML')]) {
          sh 'mvn clean package dependency:analyze -U'
        }
      }
    }
    stage('SonarQube analysis') {
      when {
        not { expression { params.RELEASE } }
      }
      steps {
        withSonarQubeEnv('GBIF Sonarqube') {
          sh 'mvn sonar:sonar'
        }
      }
    }
    stage('Snapshot to nexus') {
      when {
        allOf {
          not { expression { params.RELEASE } }
          branch 'master';
        }
      }
      steps {
        configFileProvider(
                [configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                        variable: 'MAVEN_SETTINGS_XML')]) {
          sh 'mvn clean -s $MAVEN_SETTINGS_XML deploy'
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