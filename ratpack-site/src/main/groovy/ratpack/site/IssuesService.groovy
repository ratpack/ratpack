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

import com.google.common.collect.ImmutableList

import javax.inject.Inject

@javax.inject.Singleton
class IssuesService {

  private final GitHubApi githubApi

  @Inject
  IssuesService(GitHubApi githubApi) {
    this.githubApi = githubApi
  }

  IssueSet closed(RatpackVersion version) {
    def issues = githubApi.issues(state: "closed", milestone: version.githubNumber.toString(), sort: "number", direction: "asc")
    def issuesBuilder = ImmutableList.builder()
    def pullRequestsBuilder = ImmutableList.builder()

    issues.each {
      def number = it.get("number").asInt()
      def title = it.get("title").asText()
      def user = it.get("user")
      def author = user.get("login").asText()
      def authorUrl = user.get("html_url").asText()

      def prUrl = it.get("pull_request").get("html_url")

      def url
      def builder

      if (prUrl.isNull()) {
        url = it.get("html_url").asText()
        builder = issuesBuilder
      } else {
        url = prUrl.asText()
        builder = pullRequestsBuilder
      }

      builder.add(new Issue(number, url, title, author, authorUrl))
    }

    new IssueSet(issuesBuilder.build(), pullRequestsBuilder.build())
  }

  static class Issue {
    final int number
    final String url
    final String title
    final String author
    final String authorUrl

    Issue(int number, String url, String title, String author, String authorUrl) {
      this.number = number
      this.url = url
      this.title = title
      this.author = author
      this.authorUrl = authorUrl
    }
  }

  static class IssueSet {
    final List<Issue> issues
    final List<Issue> pullRequests

    IssueSet(List<Issue> issues, List<Issue> pullRequests) {
      this.issues = issues
      this.pullRequests = pullRequests
    }
  }

}
