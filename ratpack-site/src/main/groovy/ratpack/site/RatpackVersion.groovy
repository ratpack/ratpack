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

import com.fasterxml.jackson.databind.JsonNode
import com.google.common.collect.ImmutableList
import org.pegdown.PegDownProcessor

class RatpackVersion {

  static final String TITLE_PREFIX = "release-"

  final String version
  final int githubNumber
  final String description
  final Date due
  final boolean released

  RatpackVersion(String version, int number, String description, Date due, boolean released) {
    this.version = version
    this.githubNumber = number
    this.description = description
    this.due = due
    this.released = released
  }

  static List<RatpackVersion> fromJson(JsonNode nodes) {
    def milestonesBuilder = new ImmutableList.Builder()

    for (node in nodes) {
      def title = node.get("title").asText()

      if (title.startsWith(TITLE_PREFIX) && !node.get("due_on").null) {
        def milestone = new RatpackVersion(
          title - TITLE_PREFIX,
          node.get("number").asInt(),
          node.get("description").asText(),
          DateUtil.fromGithubDateString(node.get("due_on").asText()),
          node.get("state").asText() == "closed"
        )

        milestonesBuilder.add(milestone)
      }
    }

    milestonesBuilder.build()
  }

  String dueString() {
    due.format("yyyy-MM-dd")
  }

  String getManualDownloadUrl() {
    def label = released ? version : "$version-SNAPSHOT"
    "http://oss.jfrog.org/artifactory/repo/io/ratpack/ratpack-manual/$label/ratpack-manual-${label}.zip"
  }

  String getDescriptionHtml() {
    new PegDownProcessor().markdownToHtml(description)
  }
}
