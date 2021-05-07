Release instructions.

(a) - denotes something that is a candidate for automation

## Release announcement

In-progress release notes during the cycle of development should have been written in `release-notes.md`.
Review these notes and edit as needed.  You can use Markdown.
The only truly mandatory requirement is to mention all contributors.

When you've got the release announcement set, copy it to the description of the GitHub milestone, but don't close the milestone yet.

## Pre

1. Ensure there is a next (version after what is being released) milestone on GitHub
1. Check that there are no outstanding reviews for commits for the current versions, or at least that any issues don't block the release
1. Check that there are no outstanding issues/pull requests for the development version (either implement or move them to next milestone)
1. (a) Ensure that there are no -SNAPSHOT dependencies (or if there are, that there is good reason for them)
1. Ensure the the build is passing (i.e. run `./gradlew clean build`)

## Go time…

1. (a) Update the `version` property in `ratpack.gradle` (i.e. drop the -SNAPSHOT)
1. Ensure the the build is still passing (i.e. run `./gradlew clean build`) - really isn't needed, but doesn't hurt
1. Commit with message “Version «number»”
1. Tag commit with name “v«number»” (don't push yet)
1. Build and upload the binaries: `./gradlew publishAllPublicationsToOssrhRepository` - See below for credential requirements
   1. If a non snapshot release, the staging repository must be closed to push to central, see https://central.sonatype.org/publish/release/
1. Publish to Gradle Plugin Portal: `./gradlew publishPlugins -i` - See below for credential requirements.

## Post

1. (a) Update `version` property in `ratpack.gradle` (i.e. increment the patch number and add -SNAPSHOT)
1. Update the `manualVersions` list in `ratpack-site.gradle` so the new manual is included in the site
1. (a) Update `release-notes.md` to remove the content specific to the freshly-completed release (i.e. set it back to a fresh template)
1. Commit with message 'Begin version «version»', and push (make sure you push the tag)
1. Run `./gradlew clean publishAllPublicationsToOssrhRepository` (to push the new snapshot snapshot, so it can be resolved)
1. Add the `Due Date` to the Milestone in GitHub and close it
1. Copy the release announcement to the GitHub tag description on the [GitHub releases page](https://github.com/ratpack/ratpack/releases) and publish the release
1. Get a tweet out about the release
1. For all example projects ([example-ratpack-gradle-java-app](https://github.com/ratpack/example-ratpack-gradle-java-app), [example-ratpack-gradle-groovy-app](https://github.com/ratpack/example-ratpack-gradle-groovy-app), [example-books](https://github.com/ratpack/example-books)):
    1. Update `master` branch to use the latest released version
    1. Merge `latest` branch into `master` to pick up any fixes for breaking changes in the released version

## Credentials needed

1. Ability to edit milestones for the `ratpack/ratpack` GitHub project (it might be that only admins can do this, not sure)
1. GPG credentials/config
    1. We use the Gradle Signing Plugin to sign the artifacts - See [the Gradle docs](https://docs.gradle.org/current/userguide/signing_plugin.html#N15692) for how to set this up
    1. One gotcha is forgetting to distribute your public key.  See [here](http://blog.sonatype.com/2010/01/how-to-generate-pgp-signatures-with-maven/#.U9rkY2MSS6N) for more info.  If you don't do this you will get problems when syncing to Maven central.
1. oss.sonatype.org credentials
    1. Create an account [for oss.sonatype.org](https://issues.sonatype.org/secure/Signup!default.jspa)
    1. Add a comment to [this JIRA ticket](https://issues.sonatype.org/browse/OSSRH-8283) with your new account, asking for permission to publish to `io.ratpack`.
    1. Add to ~/.gradle/gradle.properties as `ratpackOssrhUsername` and `ratpackOssrhPassword`
1. Gradle Plugin Portal config
    1. Access to the `ratpack_team` publish key and secret. (Ask John or Jeff)
