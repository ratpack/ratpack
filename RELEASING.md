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
1. Build and upload the binaries: `./gradlew artifactoryPublish --max-workers=1` - See below for credential requirements
1. Promote the binaries from oss.jfrog.org to Bintray and Maven Central
    1. Go to https://oss.jfrog.org/artifactory/webapp/#/builds/ratpack/
    1. To log in use your Bintray username and Bintray API key
    1. Find the build you just uploaded (you should be able to tell by the version number).  If you sort by "Time Built" desc it will be at the top of the list
    1. Take the buildNumber and run `./gradlew bintrayPublish -PbuildNumber=«buildNumber» -i`
    1. Confirm the publish in Bintray - The link to the bintray page is given on the success page of the previous step. Just in case it's:  https://bintray.com/ratpack/maven/ratpack/«version»/view/files/io/ratpack
    1. Publish to Maven central - click the 'Maven Central' tab on the Bintray package page
        1. Enter your user/pass - this is your oss.sonatype.org credentials
        1. Click “Close repository when done”
        1. Click “Sync”
1. Publish Lazybones templates to Bintray: `./gradlew publishAllTemplates` - See below for credential requirements.
1. Publish to Gradle Plugin Portal: `./gradlew publishPlugins -i` - See below for credential requirements.
<p>If you run this task more than once you may need to delete the published templates in Bintray first. 

## Post

1. (a) Update `version` property in `ratpack.gradle` (i.e. increment the patch number and add -SNAPSHOT)
1. Update the `manualVersions` list in `ratpack-site.gradle` so the new manual is included in the site
1. (a) Update `release-notes.md` to remove the content specific to the freshly-completed release (i.e. set it back to a fresh template)
1. Commit with message 'Begin version «version»', and push (make sure you push the tag)
1. Run `./gradlew clean artifactoryPublish` (to push the new snapshot snapshot, so it can be resolved)
1. Add the `Due Date` to the Milestone in GitHub and close it
1. Copy the release announcement to the GitHub tag description on the [GitHub releases page](https://github.com/ratpack/ratpack/releases) and publish the release
1. Get a tweet out about the release
1. For all example projects ([example-ratpack-gradle-java-app](https://github.com/ratpack/example-ratpack-gradle-java-app), [example-ratpack-gradle-groovy-app](https://github.com/ratpack/example-ratpack-gradle-groovy-app), [example-books](https://github.com/ratpack/example-books)):
    1. Update `master` branch to use the latest released version
    1. Merge `latest` branch into `master` to pick up any fixes for breaking changes in the released version

## Credentials needed

1. Ability to edit milestones for the `ratpack/ratpack` GitHub project (it might be that only admins can do this, not sure)
1. Credentials for oss.jfrog.org
    1. This is your Bintray account - use the Bintray UI to ask for write permissions to the io.ratpack group in oss.jfrog.org
1. GPG credentials/config
    1. We use the Gradle Signing Plugin to sign the artifacts (we don't let Bintray do this) - See [the Gradle docs](https://docs.gradle.org/current/userguide/signing_plugin.html#N15692) for how to set this up
    1. One gotcha is forgetting to distribute your public key.  See [here](http://blog.sonatype.com/2010/01/how-to-generate-pgp-signatures-with-maven/#.U9rkY2MSS6N) for more info.  If you don't do this you will get problems when syncing to Maven central.
1. oss.sonatype.org credentials
    1. The sync from Bintray to Central requires an account with oss.sonatype.org
    1. Create an account [for oss.sonatype.org](https://issues.sonatype.org/secure/Signup!default.jspa)
    1. Add a comment to [this JIRA ticket](https://issues.sonatype.org/browse/OSSRH-8283) with your new account, asking for permission to publish to `io.ratpack`.
1. Bintray credentials/config
    1. Lazybones templates are published to Bintray using your Bintray account.  You need to be a member of the Ratpack organization with permission to publish to the Lazybones repo.
    1. You also need Bintray credentials to publish to the Gradle Plugin Portal repo.  You need to be a member of the Ratpack organization with permission to publish to the gradle-plugins-meta repo.
    1. Create a gradle.properties file in the root of the Ratpack project and add properties for `ratpackBintrayUser` and `ratpackBintrayApiKey` with your Bintray details.  This file does not get committed.
1. Gradle Plugin Portal config
    1. Access to the `ratpack_team` publish key and secret. (Ask John or Jeff)
