package ca.bc.gov.nrids.polaris

class JenkinsUtil implements Serializable {

  private final static def envLongToShort = [
    production: "prod",
    test: "test",
    development: "dev"
  ]

  /**
   * Get NR Broker compatible cause user id from build (currentBuild)
   * - Positional parameters
   * build          object  The global currentBuild object is expected
   * defaultUser    string  The default user id
   */
  static String getCauseUserId(build, defaultUser = "unknown") {
      def userIdCause = build.getBuildCauses('hudson.model.Cause$UserIdCause')
      final String nameFromUserIdCause = userIdCause != null && userIdCause[0] != null ? userIdCause[0].userId : null
      if (nameFromUserIdCause != null) {
        return nameFromUserIdCause.toLowerCase() + "@azureidir"
      } else {
        return defaultUser ? defaultUser : 'unknown'
      }
  }

  /**
   * Get NR Broker compatible cause user id from upstream build
   * - Positional parameters
   * build          object  The global currentBuild object is expected
   * defaultUser    string  The default user id
   */
  static String getUpstreamCauseUserId(build, defaultUser = "unknown") {
    final hudson.model.Cause$UpstreamCause upstreamCause = build.rawBuild.getCause(hudson.model.Cause$UpstreamCause)
    final hudson.model.Cause$UserIdCause userIdCause = upstreamCause == null ?
        build.rawBuild.getCause(hudson.model.Cause$UserIdCause) :
        upstreamCause.getUpstreamRun().getCause(hudson.model.Cause$UserIdCause)
    final String nameFromUserIdCause = userIdCause != null ? userIdCause.userId : null
    if (nameFromUserIdCause != null) {
        return nameFromUserIdCause + "@azureidir"
    } else {
        return defaultUser ? defaultUser : 'unknown'
    }
  }

  /**
   * Converts a standard long environment name into its short version
   * - Positional parameters
   * env          string  The environment name to convert
   */
  static String convertLongEnvToShort(env) {
    return JenkinsUtil.envLongToShort[env]
  }

  static void putFile(username, password, apiURL, filePath) {
    try {
      def requestBody = new File(filePath)
      def encodeBody = URLEncoder.encode(requestBody, "UTF-8")
      def url = new URL(apiURL)
      def connection = url.openConnection()

      // Basic Authentication
      String auth = "${username}:${password}"
      String encodedAuth = Base64.getEncoder().encodeToString(auth.bytes)
      String authHeader = "Basic ${encodedAuth}"
      connection.setRequestProperty("Authorization", authHeader)

      connection.setRequestMethod("POST")
      connection.setRequestProperty("Content-Type", "application/octet-stream")
      connection.setDoOutput(true)

      OutputStream os = connection.getOutputStream()
      byte[] input = encodeBody.getBytes("utf-8")
      os.write(input, 0, input.length)

      def responseCode = connection.getResponseCode()

      println "Response Code: $responseCode"

      if (responseCode == HttpURLConnection.HTTP_OK) {
          BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))
          StringBuilder response = new StringBuilder()
          String responseLine
          while ((responseLine = br.readLine()) != null) {
              response.append(responseLine.trim())
          }
          println "Response Data: $response"
      } else {
          println "Error: ${connection.getResponseMessage()}"
      }
    } catch (Exception e) {
      println "Error Occurs: ${e.message}"
      throw e
    }
  }
}
