package ca.bc.gov.nrids.polaris

class BrokerApi implements Serializable {
  static final String BROKER_BASE_URL = "https://broker.io.nrs.gov.bc.ca/v1/"
  static final String HEADER_BROKER_TOKEN = "X-Broker-Token"

  def brokerJwt

  BrokerApi(def brokerJwt) {
    this.brokerJwt = brokerJwt
  }

// public getCollectionByIdArgs<T extends CollectionNames>(
//     name: T,
//     id: string,
//   ): { method: string; url: string; options: { responseType: 'json' } } {
//     return {
//       method: 'GET',
//       url: `${environment.apiUrl}/v1/collection/${this.stringUtil.snakecase(name)}/${id}`,
//       options: { responseType: 'json' },
//     };
//   }

  public getCollectionById(String collection, String id) {
    def jsonSlurper = new groovy.json.JsonSlurperClassic()
    def request = new URL("${this.BROKER_BASE_URL}collection/${collection}/${id}").openConnection()
    request.setRequestMethod("GET")
    request.setRequestProperty("Content-Type", "application/json")
    request.setRequestProperty("Authorization", "Bearer " + brokerJwt)
    def requestCode = request.getResponseCode()
    if (this.isResponseSuccess(requestCode)) {
      return jsonSlurper.parseText(request.getInputStream().getText())
    }
    def errorResponseBody = request.getErrorStream()?.getText() ?: "No error response body"
    def errorMessage = "Failed to get service. Response code: $getRC Response body: $errorResponseBody"
    throw new IllegalStateException(errorMessage)
  }

  public doUniqueKeyCheck(String collection, String key, String value) {
    def jsonSlurper = new groovy.json.JsonSlurperClassic()
    def request = new URL("${this.BROKER_BASE_URL}collection/${collection}/unique/${key}/${value}").openConnection()
    request.setRequestMethod("POST")
    request.setRequestProperty("Content-Type", "application/json")
    request.setRequestProperty("Authorization", "Bearer " + brokerJwt)
    def requestCode = request.getResponseCode()
    if (this.isResponseSuccess(requestCode)) {
      return jsonSlurper.parseText(request.getInputStream().getText())
    }
    def errorResponseBody = request.getErrorStream()?.getText() ?: "No error response body"
    def errorMessage = "Failed to get service. Response code: $getRC Response body: $errorResponseBody"
    throw new IllegalStateException(errorMessage)
  }

  /**
   * Open the intention
   * - Positional parameters
   * authToken   String   The JWT to send to authenticate this request
   * - Named parameters
   * quickstart: boolean  If an intention has a single action, setting this true will start the action.
   * ttl:        Number   The time to live (ttl) for the intention.
   *                      Long running processes may need to set this higher than the default.
   */
  public boolean openIntention(Map args, message) {
    def jsonSlurper = new groovy.json.JsonSlurperClassic()

    if (!brokerJwt) {
      throw new IllegalArgumentException()
    }

    // POST open
    def params = [:]
    if (args.ttl) {
      params["ttl"] = args.ttl
    }
    if (args.quickstart) {
      params["quickstart"] = "true"
    }
    def paramStr = params.collect({ it }).join('&')
    def post = new URL(this.BROKER_BASE_URL + "intention/open?" + paramStr).openConnection()
    post.setRequestMethod("POST")
    post.setDoOutput(true)
    post.setRequestProperty("Content-Type", "application/json")
    post.setRequestProperty("Authorization", "Bearer " + brokerJwt)
    post.getOutputStream().write(message.getBytes("UTF-8"))

    def postRC = post.getResponseCode()
    if (this.isResponseSuccess(postRC)) {
      return jsonSlurper.parseText(post.getInputStream().getText())
    }
    def errorResponseBody = post.getErrorStream()?.getText() ?: "No error response body"
    def errorMessage = "Failed to open intention. Response code: $postRC Response body: $errorResponseBody"
    throw new IllegalStateException(errorMessage)
  }
  def open(String message) {
    this.open([:], message)
  }

  public boolean actionLifecycleLog(String token, String type) {
    def post = new URL(this.BROKER_BASE_URL + "intention/action/" + type).openConnection()
    post.setRequestMethod("POST")
    post.setRequestProperty("Content-Type", "application/json")
    post.setRequestProperty(HEADER_BROKER_TOKEN, token)
    return this.isResponseSuccess(post.getResponseCode())
  }

  public boolean registerActionArtifact(String action, String message) {
    def post = new URL(this.BROKER_BASE_URL + "intention/action/artifact").openConnection()
    post.setRequestMethod("POST")
    post.setDoOutput(true)
    post.setRequestProperty("Content-Type", "application/json")
    post.setRequestProperty(HEADER_BROKER_TOKEN, this.openResponse.actions[action].token)
    post.getOutputStream().write(message.getBytes("UTF-8"))
    return this.isResponseSuccess(post.getResponseCode())
  }

  public boolean patchAction(String action, String message) {
    def post = new URL(this.BROKER_BASE_URL + "intention/action/patch").openConnection()
    post.setRequestMethod("POST")
    post.setDoOutput(true)
    post.setRequestProperty("Content-Type", "application/json")
    post.setRequestProperty(HEADER_BROKER_TOKEN, this.openResponse.actions[action].token)
    post.getOutputStream().write(message.getBytes("UTF-8"))
    def postRC = post.getResponseCode()
    if (!this.isResponseSuccess(postRC)) {
      def errorResponseBody = post.getErrorStream()?.getText() ?: "No error response body"
      def errorMessage = "Failed to patch. Response code: $postRC Response body: $errorResponseBody"

      throw new IllegalStateException(errorMessage)
    }
    return true
  }

  /**
   * Close the intention
   * - Positional parameters
   * successArg   boolean  Set true if all actions were a success
   */
  public String close(String token, boolean successArg) {
    // POST close
    def jsonSlurper = new groovy.json.JsonSlurperClassic()
    def post = new URL(
      this.BROKER_BASE_URL + "intention/close?outcome=" +
      (successArg ? "success": "failure")).openConnection()
    post.setRequestMethod("POST")
    post.setRequestProperty("Content-Type", "application/json")
    post.setRequestProperty(HEADER_BROKER_TOKEN, token)
    def resp = jsonSlurper.parseText(post.getInputStream().getText())
    return resp.audit
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
  public String provisionSecretId(actionToken, roleId = null) {
    def jsonSlurper = new groovy.json.JsonSlurperClassic()
    def post = new URL(this.BROKER_BASE_URL + "provision/approle/secret-id").openConnection()
    post.setRequestMethod("POST")
    post.setRequestProperty("Content-Type", "application/json")
    if (roleId) {
      post.setRequestProperty("X-Vault-Role-Id", (String) roleId)
    }
    post.setRequestProperty(HEADER_BROKER_TOKEN, (String) actionToken)
    def postRC = post.getResponseCode()
    if (!this.isResponseSuccess(postRC)) {
      def errorResponseBody = post.getErrorStream()?.getText() ?: "No error response body"
      def errorMessage = "Failed to provision secret id. Response code: $postRC Response body: $errorResponseBody"
      throw new IllegalStateException(errorMessage)
    }
    def wrappedTokenResponse = jsonSlurper.parseText(post.getInputStream().getText())
    return wrappedTokenResponse.wrap_info.token
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
  public String provisionToken(actionToken, roleId = null, unwrapToken = true) {
    def jsonSlurper = new groovy.json.JsonSlurperClassic()
    def post = new URL(this.BROKER_BASE_URL + "provision/token/self").openConnection()
    post.setRequestMethod("POST")
    post.setRequestProperty("Content-Type", "application/json")
    if (roleId) {
      post.setRequestProperty("X-Vault-Role-Id", (String) roleId)
    }
    post.setRequestProperty(HEADER_BROKER_TOKEN, (String) actionToken)
    def postRC = post.getResponseCode()
    if (!this.isResponseSuccess(postRC)) {
      def errorResponseBody = post.getErrorStream()?.getText() ?: "No error response body"
      def errorMessage = "Failed to provision token. Response code: $postRC Response body: $errorResponseBody"

      throw new IllegalStateException(errorMessage)
    }
    def wrappedTokenResponse = jsonSlurper.parseText(post.getInputStream().getText())

    if (!unwrapToken) {
      return wrappedTokenResponse.wrap_info.token
    }

    return Vault.unwrapToken(wrappedTokenResponse.wrap_info.token)
  }

  private boolean isResponseSuccess(code) {
    return code >= 200 && code <= 299
  }
}

