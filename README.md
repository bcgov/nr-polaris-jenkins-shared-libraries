# polaris-shared-libraries

## Overview
This repository contains the Polaris Jenkins Shared Library used across BC Gov Polaris Jenkins pipelines. It provides pipeline steps, helper classes (e.g. Podman, BrokerIntention, Vault, BrokerApi), and common utilities.

## Releasing a new library version 
1. Ensure tests and pipeline validation pass (locally or in CI).
2. Pick the release tag (semantic version recommended), e.g. v1.2.3.
3. Create a GitHub release (requires GitHub CLI):
   gh release create v1.2.3 -t "v1.2.3" -n "Release notes here"
   Or
   You can use Github web to draft a new release to publish a new release

Notes:
- Jenkins can be configured to load this shared library by tag (recommended for reproducible pipelines).

## Update Jenkins pipeline repo to use the new library tag
Pipelines reference the shared library in Jenkinsfiles, for example:

  @Library('polaris@v1.0.0')

After creating the new tag, update pipeline repos to point to the new tag (v1.0.0).

Recommended approach:
1. From Polaris Jenkins pipeline repo(https://github.com/bcgov/nr-polaris-pipelines), run an update script that replaces the library tag for all Jenkinsfile. In scripts folder, there is a script file named `update_polaris_library_tag.sh`, run:
```
   cd /path/to/nr-polaris-pipelines
   ./scripts/update_polaris_library_tag.sh
   Enter the polaris library tag or branch (e.g., v1.2.3): v1.0.0
```

2. Commit and push the changes, then run the pipeline to validate.

3. Create a GitHub release (requires GitHub CLI) on Jenkins pipeline repo or use GitHub web side draft a new release to publish a new release

# References
- Jenkins pipeline shared libraries documentation: https://www.jenkins.io/doc/book/pipeline/shared-libraries/
- GitHub CLI (optional): https://cli.github.com/
