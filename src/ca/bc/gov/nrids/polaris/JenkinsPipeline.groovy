package ca.bc.gov.nrids.polaris

class JenkinsPipeline implements Serializable {

  def script

  JenkinsPipeline(def script) {
    // Store the script object to call functions implicitly used in the Jenkinsfile
    this.script = script
  }

  /**
   * Print a banner with job information and links to documentation
   * - Positional parameters
   * env             map     The environment map (usually 'env' in the Jenkinsfile)
   * pipelineFolder  string  The folder name of the pipeline in the polaris-pipelines repository
   */
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

  /**
   * Retrieve the Ansible inventory from the dev-all-in-one repository
   * - Named parameters
   * branch   string  The git branch to checkout (default: 'release/1.0.0')
   */
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

  /**
   * Retrieve an Ansible collection from a git repository URL
   * - Positional parameters
   * podman   object  A Podman object to run the container
   * url      string  The git repository URL of the Ansible collection
   * path     string  The path to install the collection to
   */
  def retrieveAnsibleCollection(podman, url, path) {
    podman.run("willhallonline/ansible:2.16-alpine-3.21",
        options: "-v \$(pwd):/ansible",
        command: "/bin/sh -c \"git config --global advice.detachedHead false && ansible-galaxy collection install ${url} -p ${path}\"")
  }

  /**
   * Login to Vault using the intention and prepare Podman by logging into the container registry
   * - Positional parameters
   * intention     object  An Intention object to use for logging into Vault
   * podman        object  A Podman object to run the container
   * env           map     A map to populate with environment variables read from Vault
   * loginAction   string  The action name to use for logging into Vault (default: "login")
   */
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
