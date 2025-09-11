package ca.bc.gov.nrids.polaris

class JenkinsPipeline implements Serializable {

  def script

  JenkinsPipeline(def script) {
    this.script = script
  }

  def retrieveInventory(Map config = [:]) {
      script.stage('Checkout INFRA dev-all-in-one') {
        script.steps {
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
