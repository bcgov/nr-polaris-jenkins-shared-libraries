package ca.bc.gov.nrids.polaris

class JenkinsRepo implements Serializable {

  def script

  JenkinsRepo(def script) {
    // Store the script object to call functions implicitly used in the Jenkinsfile
    this.script = script
  }

  /**
   * Setup git sparse checkout for repository
   * - Positional parameters
   * gitRepo        string  The git repository url
   * gitBasicAuth   object  The Jenkins credentials object for basic auth (username:token)
   * gitTag         string  The git tag or branch to checkout
   * files          list    List of files or directories to include in sparse checkout
   */
  def setupSparseCheckout(gitRepo, gitBasicAuth, gitTag, files = ['.jenkins', 'catalog-info.yaml']) {
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

  /**
   * Retrieve all catalog files in the repository, including following targets in Location files
   * Returns a list of catalog file paths
   */
  def retrieveRepoCatalogs() {
    def catalogs = ['catalog-info.yaml']

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

  /**
   * Get the playbook path from catalog metadata annotations, or default to 'playbooks'
   * - Positional parameters
   * catalogInfo   map   The catalog info map as returned by findServiceInCatalogs()
   */
  def getPlaybookPath(catalogInfo) {
    def playbookPath = catalogInfo.catalog.metadata.annotations['playbook.io.nrs.gov.bc.ca/playbookPath'] ?: 'playbooks'
    return "${catalogInfo.dir}/${playbookPath}"
  }

  /**
   * Retrieve the service playbook directory by adding it to sparse checkout
   * - Positional parameters
   * catalogInfo   map   The catalog info map as returned by findServiceInCatalogs()
   */
  def retrieveServicePlaybookDir(catalogInfo) {
    def playbookPath = getPlaybookPath(catalogInfo)
    script.sh """
      git sparse-checkout add ${playbookPath}
      git sparse-checkout reapply
    """
    return playbookPath
  }

  /**
   * Find the service in the list of catalog files
   * - Positional parameters
   * catalogs   list    List of catalog file paths
   * service    string  The service name to find
   * Returns a map with keys: path, dir, catalog
   * or null if not found
   */
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
