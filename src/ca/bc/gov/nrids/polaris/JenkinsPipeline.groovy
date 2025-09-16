package ca.bc.gov.nrids.polaris

class JenkinsPipeline implements Serializable {

  def script

  JenkinsPipeline(def script) {
    // Store the script object to call functions implicitly used in the Jenkinsfile
    this.script = script
  }

  def banner(env, pipelineFolder) {
    script.echo """
    ====================================================
    -                NR Polaris Pipeline               -
    ====================================================

    Job:    ${env.JOB_NAME}
    Build:  #${env.BUILD_NUMBER}
    URL:    ${env.BUILD_URL}

    Service control pipeline:
    https://github.com/bcgov-nr/polaris-pipelines

    Jenkinsfile:
    https://github.com/bcgov-nr/polaris-pipelines/blob/main/pipelines/${pipelineFolder}/Jenkinsfile

    Ansible Polaris collection:
    https://github.com/bcgov/nr-polaris-collection

    Developers are expected to review the documentation
    of all roles used by their Ansible playbook file.
    ====================================================
    """.replaceAll("(?m)^[ \t]+", "")
  }

  def retrieveAnsibleInventory(Map config = [:]) {
    // script.stage('Checkout INFRA dev-all-in-one') {
      script.checkout([
        $class: 'GitSCM',
        branches: [[name: config.branch ?: 'release/1.0.0']],
        doGenerateSubmoduleConfigurations: false,
        submoduleCfg: [],
        userRemoteConfigs: [
          [
            credentialsId: 'ci-user',
            url: 'https://bwa.nrs.gov.bc.ca/int/stash/scm/infra/dev-all-in-one.git'
          ]
        ]
      ])
    // }
  }

  def retrieveAnsibleCollection(podman, url, path) {
    podman.run("willhallonline/ansible:2.16-alpine-3.21",
        options: "-v \$(pwd):/ansible",
        command: "/bin/sh -c \"git config --global advice.detachedHead false && ansible-galaxy collection install ${url} -p ${path}\"")
  }

  def loginAndPreparePodman(intention, podman, env, loginAction = "login") {
    intention.startAction(loginAction)

    def vaultToken = intention.provisionToken(
      loginAction,
      script.credentials('knox-jenkins-jenkins-apps-prod-role-id'))
    def vault = new Vault(vaultToken)
    vault.readToObject("apps/data/prod/jenkins/jenkins-apps/artifactory", env)
    vault.readToObject("apps/data/prod/jenkins/jenkins-apps/cdua", env)
    vault.revokeToken()

    podman.login(options: "-u ${env.REGISTRY_USERNAME} -p ${env.REGISTRY_PASSWORD}")
  }\
}
