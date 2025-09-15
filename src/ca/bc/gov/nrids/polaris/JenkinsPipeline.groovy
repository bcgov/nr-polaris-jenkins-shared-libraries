package ca.bc.gov.nrids.polaris

class JenkinsPipeline implements Serializable {

  def script

  JenkinsPipeline(def script) {
    // Store the script object to call functions implicitly used in the Jenkinsfile
    this.script = script
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
        command: '/bin/sh -c "git config --global advice.detachedHead false && ansible-galaxy collection install ${url} -p ${path}"')
  }

  def setupSparseCheckout(gitRepo, gitBasicAuth, files, gitTag) {
    def GIT_REPO = gitRepo.replaceFirst(/^https?:\/\//, '')
    def GIT_BRANCH = gitTag ?: 'main'
    def basicAuthParts = gitBasicAuth?.getPlainText().split(':')
    def varPasswordPairs = []
    if (basicAuthParts.length > 1) {
      varPasswordPairs = [[var: 'GITHUB_TOKEN', password: basicAuthParts[1]]]
    }
    script.wrap([
      $class: 'MaskPasswordsBuildWrapper',
      varPasswordPairs: [[var: 'GITHUB_TOKEN', password: script.env.GITHUB_TOKEN]]
    ]) {
      script.sh """
          git config --global advice.detachedHead false
          git clone -q --no-checkout https://${basicAuthParts.length > 1 ? gitBasicAuth.getPlainText() + '@' : ''}${GIT_REPO} .
          git sparse-checkout init --cone
          git sparse-checkout set ${files.join(' ')}
          git checkout ${GIT_BRANCH}
      """
    }
  }
// params.gitRepo
// params.gitBasicAuth
// params.gitTag

  def retrieveRepoRoot(Map config = [:]) {
    this.retrieveRepoRoot(
      config.gitRepo,
      config.gitBasicAuth,
      config.gitTag
    )
  }

  def retrieveRepoCatalogs(gitRepo, gitBasicAuth, gitTag) {
    def catalogs = ['catalog-info.yaml']
    setupSparseCheckout(gitRepo, gitBasicAuth, ['.jenkins', 'catalog-info.yaml'], gitTag)

    def catalog = script.readYaml(file: 'catalog-info.yaml')
    if (catalog.kind == 'Location') {
      echo "catalog-info.yaml is a Location file. Adding targets..."
      def targets = catalog.spec.targets.collect { it.target }
      script.sh """
        git sparse-checkout add ${targets.join(' ')}
        git sparse-checkout reapply
      """
      catalogs += targets
    } else if (catalog.kind == 'Component') {
      script.echo "catalog-info.yaml is a Component file. No targets to follow."
    } else {
      script.error "catalog-info.yaml is neither a Location nor a Component file. Cannot proceed."
    }
    script.echo "Catalog files found: ${catalogs}"
    return catalogs;
  }

  def getPlaybookPath(catalogInfo) {
    def playbookPath = catalogInfo.catalog.metadata.annotations['playbook.io.nrs.gov.bc.ca/playbookPath'] ?: 'playbooks'
    return "${catalogInfo.dir}/${playbookPath}"
  }

  def retrieveServicePlaybooks(catalogInfo) {
    def playbookPath = getPlaybookPath(catalogInfo)
    script.sh """
      git sparse-checkout add ${playbookPath}
      git sparse-checkout reapply
    """
    return playbookPath
  }

  def findServiceInCatalogs(catalogs, service) {
    for (catalogFile in catalogs) {
      def catalog = script.readYaml(file: catalogFile)

      if (catalog.kind == 'Component' && catalog.metadata.name.toString() == service.toString()) {
        return [
          path: catalogFile,
          dir: catalogFile.replaceFirst('/?catalog-info.yaml$', '')  ?: '.',
          catalog: catalog
        ]
      }
    }
    return null
  }

}
