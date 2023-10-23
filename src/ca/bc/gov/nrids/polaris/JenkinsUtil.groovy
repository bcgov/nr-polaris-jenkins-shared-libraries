package ca.bc.gov.nrids.polaris

class JenkinsUtil implements Serializable {
  /**
   * Get NR Broker compatible cause user id from build (currentBuild)
   * - Positional parameters
   * build          object  The global currentBuild object is expected
   */
  static String getCauseUserId(build) {
      def userIdCause = build.getBuildCauses('hudson.model.Cause$UserIdCause');
      final String nameFromUserIdCause = userIdCause != null && userIdCause[0] != null ? userIdCause[0].userId : null;
      if (nameFromUserIdCause != null) {
          return nameFromUserIdCause + "@azureidir";
      } else {
          return 'unknown'
      }
  }

  /**
   * Converts a standard long environment name into its short version
   * - Positional parameters
   * env          string  The environment name to convert
   */
  static String convertLongEnvToShort(env) {
      envLongToShort = [:]
      envLongToShort["production"] = "prod"
      envLongToShort["test"] = "test"
      envLongToShort["development"] = "dev"
      return envLongToShort[env]
  }
}
