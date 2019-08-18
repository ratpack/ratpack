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

import com.fasterxml.jackson.databind.JsonNode
import com.google.common.collect.ImmutableList
import groovy.transform.CompileStatic
import ratpack.exec.Promise

import javax.inject.Inject

@CompileStatic
@com.google.inject.Singleton
class ApiBackedGitHubData implements GitHubData {

  private final GitHubApi gitHubApi

  @Inject
  ApiBackedGitHubData(GitHubApi gitHubApi) {
    this.gitHubApi = gitHubApi
  }

  @Override
  Promise<IssueSet> closed(RatpackVersion version) {
    gitHubApi.issues(state: "closed", milestone: version.githubNumber.toString(), sort: "number", direction: "asc").map { JsonNode issues ->
      def issuesBuilder = ImmutableList.builder()
      def pullRequestsBuilder = ImmutableList.builder()

      issues.each { JsonNode it ->
        def number = it.get("number").asInt()
        def title = it.get("title").asText()
        def user = it.get("user")
        def author = user.get("login").asText()
        def authorUrl = user.get("html_url").asText()

        def prUrl = it.get("pull_request")?.get("html_url")

        def url
        def builder

        if (prUrl != null && !prUrl.isNull()) {
          url = prUrl.asText()
          builder = pullRequestsBuilder
        } else {
          url = it.get("html_url").asText()
          builder = issuesBuilder
        }

        builder.add(new Issue(number, url, title, author, authorUrl))
      }

      new IssueSet(issuesBuilder.build(), pullRequestsBuilder.build())
    }
  }

  @Override
  void forceRefresh() {

  }

  Promise<List<RatpackVersion>> getReleasedVersions() {
    gitHubApi.milestones(state: "closed", sort: "due_date", direction: "desc").map {
      RatpackVersion.fromJson(it as JsonNode) as List
    }
  }

  Promise<List<RatpackVersion>> getUnreleasedVersions() {
    gitHubApi.milestones(state: "open", sort: "due_date", direction: "asc").map {
      RatpackVersion.fromJson(it as JsonNode) as List
    }
  }

}

