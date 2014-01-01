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

import javax.inject.Inject

@javax.inject.Singleton
class IssuesService {

  private final GitHubApi githubApi

  @Inject
  IssuesService(GitHubApi githubApi) {
    this.githubApi = githubApi
  }

  List<Issue> closed(RatpackVersion version) {
    githubApi.issues(state: "closed", milestone: version.githubNumber.toString(), sort: "number", direction: "asc").collect {
      new Issue(
        it.get("number").asInt(),
        it.get("html_url").asText(),
        it.get("title").asText()
      )
    }
  }

}
