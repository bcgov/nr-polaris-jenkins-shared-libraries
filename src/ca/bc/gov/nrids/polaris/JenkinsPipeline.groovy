package ca.bc.gov.nrids.polaris

class JenkinsPipeline implements Serializable {

  static String retrieveInventory(Map config = [:]) {
      stage('Checkout INFRA dev-all-in-one') {
        steps {
          checkout([
            $class: 'GitSCM',
            branches: [[name: config.branch ?: 'release/1.0.0']],
            doGenerateSubmoduleConfigurations: false,
            extensions: [
              [$class: 'RelativeTargetDirectory', relativeTargetDir: "infra"]
            ],
            submoduleCfg: [],
            userRemoteConfigs: [
              [
                credentialsId: 'ci-user',
                url: "https://bwa.nrs.gov.bc.ca/int/stash/scm/infra/dev-all-in-one.git"
              ]
            ]
        ])
      }
    }
  }
}
