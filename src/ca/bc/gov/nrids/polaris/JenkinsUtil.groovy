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
}
