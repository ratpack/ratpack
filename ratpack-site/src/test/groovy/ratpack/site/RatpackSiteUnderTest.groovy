/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.site

import ratpack.exec.ExecControl
import ratpack.groovy.test.LocalScriptApplicationUnderTest
import ratpack.site.github.GitHubData
import ratpack.site.github.MockGithubData
import ratpack.site.github.RatpackVersion
import ratpack.site.github.RatpackVersions
import ratpack.test.remote.RemoteControl


class RatpackSiteUnderTest extends LocalScriptApplicationUnderTest {

  final RemoteControl remote = new RemoteControl(this)

  RatpackSiteUnderTest() {
    super(
      "other.remoteControl.enabled": "true",
      ("other.${SiteModule.GITHUB_ENABLE}".toString()): "false"
    )
  }

  void mockGithubData() {
    remote.exec {
      def data = new MockGithubData()

      def config = new TestConfig()

      config.manualVersions.eachWithIndex{ version, index ->
        data.released.add(new RatpackVersion(version, index + 1, "foo", new Date(), true))
      }

      data.unreleased.add(new RatpackVersion(config.currentVersion - "-SNAPSHOT" , config.manualVersions.size() + 1, "foo", new Date(), false))

      add(GitHubData, data)
      add(new RatpackVersions(data, get(ExecControl)))
    }
  }

}
