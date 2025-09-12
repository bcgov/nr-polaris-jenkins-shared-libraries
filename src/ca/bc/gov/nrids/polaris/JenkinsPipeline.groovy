package ca.bc.gov.nrids.polaris

class JenkinsPipeline implements Serializable {

  def script

  JenkinsPipeline(def script) {
    this.script = script
  }

  def retrieveInventory(Map config = [:]) {
    script.stage('Checkout INFRA dev-all-in-one') {
      script.checkout([
        $class: 'GitSCM',
        branches: [[name: config.branch ?: 'release/1.0.0']],
        doGenerateSubmoduleConfigurations: false,
        extensions: [
          [$class: 'RelativeTargetDirectory', relativeTargetDir: 'infra']
        ],
        submoduleCfg: [],
        userRemoteConfigs: [
          [
            credentialsId: 'ci-user',
            url: 'https://bwa.nrs.gov.bc.ca/int/stash/scm/infra/dev-all-in-one.git'
          ]
        ]
      ])
    }
  }

// params.gitRepo
// params.gitBasicAuth
// params.gitTag

  def retrieveRepoRoot(Map config = [:]) {
    script.dir('app') {
      def GIT_REPO = config.gitRepo.replaceFirst(/^https?:\/\//, '')
      def GIT_BRANCH = config.gitTag ?: 'main'
      def basicAuthSplit = config.gitBasicAuth?.split(':')
      def varPasswordPairs = []
      if (basicAuthSplit?.size() == 3) {
        varPasswordPairs = [[var: 'GITHUB_TOKEN', password: basicAuthSplit[2]]]
      }
      script.wrap([
        $class: 'MaskPasswordsBuildWrapper',
        varPasswordPairs: [[var: 'GITHUB_TOKEN', password: script.env.GITHUB_TOKEN]]
      ]) {
        sh """
            git config --global advice.detachedHead false
            git clone -q --no-checkout https://${config.gitBasicAuth ? config.gitBasicAuth + '@' : ''}${GIT_REPO} .
            git sparse-checkout set .jenkins catalog-info.yaml
            git checkout ${GIT_BRANCH}
            ls -la
        """
      }
    }
  }
}
