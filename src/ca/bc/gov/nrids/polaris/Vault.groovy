package ca.bc.gov.nrids.polaris

class Vault implements Serializable {
  def token
  def baseUrl
  static final String KNOX_URL = "https://knox.io.nrs.gov.bc.ca"
  static final String KNOX_API_URL = "https://knox.io.nrs.gov.bc.ca/v1/"
  static final String HEADER_VAULT_TOKEN = "X-Vault-Token"

  Vault(String token, String baseUrl = KNOX_API_URL) {
    this.token = token
    this.baseUrl = baseUrl
  }

  /**
   * Unwraps the token and returns it
   * - Positional parameters
   * token   String  The token to unwrap
   */
  static String unwrapToken(String token, baseUrl = KNOX_API_URL) {
    def post = new URL(baseUrl + "sys/wrapping/unwrap").openConnection()
    post.setRequestMethod("POST")
    post.setRequestProperty("Content-Type", "application/json")
    post.setRequestProperty(HEADER_VAULT_TOKEN, token)
    if (!this.isResponseSuccess(post.getResponseCode())) {
      return
    }
    def jsonSlurper = new groovy.json.JsonSlurperClassic()
    def tokenResponse = jsonSlurper.parseText(post.getInputStream().getText())

    return tokenResponse.auth.client_token
  }

  /**
   * Reads the data at the path.
   * - Positional parameters
   * path   String  The path to the secret to read
   * Note: kv secrets are located at <mount>/data/<path to secret>
   */
  public Object read(String path) {
    def req = new URL(this.baseUrl + path).openConnection()
    req.setRequestMethod("GET")
    req.setRequestProperty("Content-Type", "application/json")
    req.setRequestProperty(HEADER_VAULT_TOKEN, token)

    if (!this.isResponseSuccess(req.getResponseCode())) {
      return
    }
    def jsonSlurper = new groovy.json.JsonSlurperClassic()
    def resp = jsonSlurper.parseText(req.getInputStream().getText())

    return resp.data.data
  }

  /**
   * Read to object
   * - Positional parameters
   * path   String  The path to the secret to read
   * Note: kv secrets are located at <mount>/data/<path to secret>
   * object String  The object to set the key/values on
   * - Named parameters
   * keyTransform: Closure    If set, call closure to compute key
   */
  def readToObject(Map args, String path, Object obj) {
    def data = this.read(path);
    if (data) {
      data.each { obj[ args.keyTransform ? args.keyTransform(it.key) : it.key] = it.value }
    }
  }
  def readToObject(String path, Object obj) {
    this.run([:], path, obj)
  }

  /**
   * Revokes the token
   */
  public int revokeToken() {
    def req = new URL(this.baseUrl + "auth/token/revoke-self").openConnection()
    req.setRequestMethod("POST")
    req.setRequestProperty("Content-Type", "application/json")
    req.setRequestProperty(HEADER_VAULT_TOKEN, token)
    return req.getResponseCode()
  }

  private static boolean isResponseSuccess(code) {
    return code >= 200 && code <= 299
  }
}