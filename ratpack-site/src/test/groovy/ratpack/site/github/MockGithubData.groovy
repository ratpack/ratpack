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

package ratpack.site.github

import ratpack.exec.ExecControl
import ratpack.exec.Promise

class MockGithubData implements GitHubData {

  final List<RatpackVersion> released = []
  final List<RatpackVersion> unreleased = []

  @Override
  Promise<List<RatpackVersion>> getReleasedVersions() {
    ExecControl.current().promiseOf(released)
  }

  @Override
  Promise<List<RatpackVersion>> getUnreleasedVersions() {
    ExecControl.current().promiseOf(unreleased)
  }

  @Override
  Promise<IssueSet> closed(RatpackVersion version) {
    ExecControl.current().promiseOf(new IssueSet([], []))
  }

}
