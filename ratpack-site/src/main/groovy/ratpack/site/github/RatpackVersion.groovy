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
import org.pegdown.PegDownProcessor

import java.text.SimpleDateFormat

@CompileStatic
class RatpackVersion {

  static final String TITLE_PREFIX = "release-"

  final String version
  final Integer githubNumber
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
      String title = node.get("title").asText()

      if (title.startsWith(TITLE_PREFIX)) {
        def milestone = new RatpackVersion(
          title.substring(TITLE_PREFIX.length()),
          node.get("number").asInt(),
          node.get("description").asText(),
          fromGithubDateString(node.get("due_on").asText()),
          node.get("state").asText() == "closed"
        )

        milestonesBuilder.add(milestone)
      }
    }

    milestonesBuilder.build()
  }

  String dueString() {
    due ? new SimpleDateFormat("yyyy-MM-dd").format(due) : "unscheduled"
  }

  String getManualDownloadUrl() {
    def label = released ? version : "$version-SNAPSHOT"
    def repo = released ? "libs-release" : "libs-snapshot"

    return "https://oss.jfrog.org/artifactory/$repo/io/ratpack/ratpack-manual/$label/ratpack-manual-${label}.zip"
  }

  String getDescriptionHtml() {
    new PegDownProcessor().markdownToHtml(description)
  }

  private static Date fromGithubDateString(String str) {
    if (str == null || str.empty || str == "null") {
      null
    } else {
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(str)
    }
  }

  @Override
  String toString() {
    return "RatpackVersion{" +
      "version='" + version + '\'' +
      ", githubNumber=" + githubNumber +
      ", due=" + due +
      ", released=" + released +
      '}'
  }
}
