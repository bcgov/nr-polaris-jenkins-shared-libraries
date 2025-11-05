package ca.bc.gov.nrids.polaris

class BrokerIntention implements Serializable {
  Object intention
  Object openResponse
  BrokerApi brokerApi
  static final String BROKER_BASE_URL = "https://broker.io.nrs.gov.bc.ca/v1/"
  static final String HEADER_BROKER_TOKEN = "X-Broker-Token"

  BrokerIntention(String brokerJwt, Object intention) {
    this.intention = intention
    this.brokerApi = new BrokerApi(brokerJwt)
  }

  /**
   * Reads the intention json from the path and returns a new BrokerIntention.
   * - Positional parameters
   * path   String  The path to the file to read
   */
  static BrokerIntention fromFile(String brokerJwt, String path) {
    def jsonSlurper = new groovy.json.JsonSlurperClassic()
    return new BrokerIntention(brokerJwt, jsonSlurper.parseText(new File(path).text))
  }

  // Convenience getter/setter for user id & name, event url and event provider
  def getUserId() {
    return intention.user.id
  }
  def setUserId(user) {
    intention.user.id = (String) user
  }

  def getUserName() {
    return intention.user.name
  }
  def setUserName(user) {
    intention.user.name = (String) user
  }

  def getEventUrl() {
    return intention.event.url
  }
  def setEventUrl(url) {
    intention.event.url = (String) url
  }

  def getReason() {
    return intention.event.reason
  }
  def setReason(reason) {
    intention.event.reason = (String) reason
  }

  def getEventProvider() {
    return intention.event.provider
  }
  def setEventProvider(provider) {
    intention.event.provider = (String) provider
  }

  // Getters and setters for event.trigger fields
  def getEventTriggerId() {
    return intention.event?.trigger?.id
  }
  def setEventTriggerId(id) {
    if (!intention.event.trigger) intention.event.trigger = [:]
    intention.event.trigger.id = (String) id
  }

  def getEventTriggerName() {
    return intention.event?.trigger?.name
  }
  def setEventTriggerName(name) {
    if (!intention.event.trigger) intention.event.trigger = [:]
    intention.event.trigger.name = (String) name
  }

  def getEventTriggerUrl() {
    return intention.event?.trigger?.url
  }
  def setEventTriggerUrl(url) {
    if (!intention.event.trigger) intention.event.trigger = [:]
    intention.event.trigger.url = (String) url
  }


  /**
   * Chainable event details setter
   * - Named parameters
   * userName:                    string Sets username
   * url:                         string Sets event url
   * reason:                      string Sets event reason
   * provider:                    string Sets event provider
   * serviceName                  string Sets service name
   * triggerId:                   string Sets event.trigger.id
   * triggerName:                 string Sets event.trigger.name
   * triggerUrl:                  string Sets event.trigger.url
   * serviceProject               string Sets service project
   * environment:                 string Sets all action environments to this value
   * packageInstallationVersion:  string Sets all "package-installation" actions package.version to this value
   * serviceTargetEnvironment:    string Sets all action service target environments to this value
   */
  public BrokerIntention setEventDetails(Map args) {
    if (args.userName) {
      this.setUserName(args.userName);
    }
    if (args.url) {
      this.setEventUrl(args.url);
    }
    if (args.provider) {
      this.setEventProvider(args.provider);
    }
    if (args.reason) {
      this.setReason(args.reason);
    }
    if (args.triggerId) {
      this.setEventTriggerId(args.triggerId)
    }
    if (args.triggerName) {
      this.setEventTriggerName(args.triggerName)
    }
    if (args.triggerUrl) {
      this.setEventTriggerUrl(args.triggerUrl)
    }
    if (args.serviceName) {
      for (action in this.intention.actions) {
        action.service.name = args.serviceName
      }
    }
    if (args.serviceProject) {
      for (action in this.intention.actions) {
        action.service.project = args.serviceProject
      }
    }
    if (args.environment) {
      for (action in this.intention.actions) {
        action.service.environment = (String) args.environment
      }
    }
    this.updatePackageForAction("package-build", "buildGuid", args.packageBuildBuildGuid)
    this.updatePackageForAction("package-build", "buildNumber", args.packageBuildBuildNumber)
    this.updatePackageForAction("package-build", "buildVersion", args.packageBuildBuildVersion)
    this.updatePackageForAction("package-build", "description", args.packageBuildDescription)
    this.updatePackageForAction("package-build", "name", args.packageBuildName)
    this.updatePackageForAction("package-build", "type", args.packageBuildType)
    this.updatePackageForAction("package-build", "version", args.packageBuildVersion)
    // Update installation package
    this.updatePackageForAction("package-installation", "name", args.packageInstallationName)
    this.updatePackageForAction("package-installation", "version", args.packageInstallationVersion)
    // Update installation source
    if (args.packageInstallationSourceIntention) {
      for (action in this.intention.actions) {
        if (action.action == "package-installation") {
          action.source.intention = args.packageInstallationSourceIntention
        }
      }
    }
    // Update service target environment
    if (args.serviceTargetEnvironment) {
      for (action in this.intention.actions) {
        if (action.action == "server-access") {
          action.service.target.environment = args.serviceTargetEnvironment
        }
      }
    }

    return this
  }

  public void filterActionsByIds(ids) {
    // ensure ids is a Set for faster lookup
    def idSet = ids as Set
    intention.actions = intention.actions.findAll { obj -> idSet.contains(obj.id) }
  }

  /**
   * Open the intention
   * - Named parameters
   * quickstart: boolean  If an intention has a single action, setting this true will start the action.
   * ttl:        Number   The time to live (ttl) for the intention.
   *                      Long running processes may need to set this higher than the default.
   */
  public boolean open(Map args) {
    def message = groovy.json.JsonOutput.toJson(intention)
    this.openResponse = this.brokerApi.openIntention(args, message)
  }
  def open() {
    this.open([:])
  }

  /**
   * Start the action
   * - Positional parameters
   * action   String  The action id from the intention
   */
  public void startAction(String action) {
    if (!this.openResponse) {
      throw new IllegalStateException()
    }
    if (!action || !this.openResponse.actions[action]) {
      throw new IllegalArgumentException()
    }
    this.brokerApi.actionLifecycleLog(this.openResponse.actions[action].token, "start")
  }

  /**
   * End the action
   * - Positional parameters
   * action   String  The action id from the intention
   */
  public void endAction(String action) {
    if (!this.openResponse) {
      throw new IllegalStateException()
    }
    if (!action || !this.openResponse.actions[action]) {
      throw new IllegalArgumentException()
    }
    this.brokerApi.actionLifecycleLog(this.openResponse.actions[action].token, "end")
  }

  public boolean registerActionArtifact(String action, String message) {
    if (!this.openResponse) {
      throw new IllegalStateException()
    }
    if (!action || !this.openResponse.actions[action]) {
      throw new IllegalArgumentException()
    }
    return this.brokerApi.registerActionArtifact(this.openResponse.actions[action].token, action, message)
  }

  public boolean patchAction(String action, String message) {
    if (!this.openResponse) {
      throw new IllegalStateException()
    }
    if (!action || !this.openResponse.actions[action]) {
      throw new IllegalArgumentException()
    }
    return this.brokerApi.patchAction(this.openResponse.actions[action].token, action, message)
  }

  /**
   * Close the intention
   * - Positional parameters
   * successArg   boolean  Set true if all actions were a success
   */
  public String close(boolean successArg) {
    // Ignore close if there is no open response
    if (!this.openResponse) {
      throw new IllegalStateException("Intention was never opened")
    }
    return this.brokerApi.close(this.openResponse.token, successArg)
  }

  /**
   * Provision a secret id that can be used by the service
   * to login as the service to vault.
   * - Positional parameters
   * action   String  The action id from the intention
   * roleId   String  The service role id
   *
   * Note: The return value is wrapper token that should be
   * unwrapped only on the server using it.
   *
   * Unwrap example: vault unwrap -field=secret_id <token>
   */
  public String provisionSecretId(action, roleId = null) {
    if (!action) {
      throw new IllegalArgumentException()
    }
    if (!this.openResponse) {
      throw new IllegalStateException()
    }
    def actionToken = this.openResponse?.actions[action].token
    if (!actionToken) {
      throw new IllegalArgumentException()
    }
    return this.brokerApi.provisionSecretId(actionToken, roleId)
  }

  /**
   * Provision a (temporary) token to use with vault.
   * - Positional parameters
   * action       String  The action id from the intention
   * roleId       String  The service role id
   * unwrapToken  Boolean True if the token should be unwrapped for you.
   *
   * Note: The optional unwrapToken must be set false if the caller does
   * use the token directly. Unwrapped token must never be saved to disk or
   * transmitted.
   */
  public String provisionToken(action, roleId = null, unwrapToken = true) {
    if (!action) {
      throw new IllegalArgumentException()
    }
    if (!this.openResponse) {
      throw new IllegalStateException()
    }
    def actionToken = this.openResponse?.actions[action].token
    if (!actionToken) {
      throw new IllegalArgumentException()
    }
    return this.brokerApi.provisionToken(actionToken, roleId, unwrapToken)
  }

  private void updatePackageForAction(String actionName, String key, String value) {
    if (value) {
      for (action in this.intention.actions) {
        if (action.action == actionName) {
          action.package[key] = value
        }
      }
    }
  }
}

