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
import com.fasterxml.jackson.databind.ObjectReader
import ratpack.exec.Promise
import ratpack.http.client.HttpClient

@javax.inject.Singleton
class GitHubApi {

  private final String api
  private final String authToken
  private final GithubRequester requester

  GitHubApi(String api, String authToken, ObjectReader objectReader, HttpClient httpClient) {
    this.api = api
    this.authToken = authToken
    this.requester = new GithubRequester(objectReader, httpClient)
  }

  private Promise<JsonNode> get(String path, Map<String, String> params) {
    requester.request(api + path + toQueryString(params))
  }

  Promise<JsonNode> milestones(Map<String, String> params) {
      get("repos/ratpack/ratpack/milestones", params)
  }

  Promise<JsonNode> issues(Map<String, String> params) {
    get("repos/ratpack/ratpack/issues", params)
  }

  private String toQueryString(Map<String, String> params) {
    if (authToken) {
      params += [access_token: authToken]
    }
    "?" + (
      (params + [per_page: "100"]).collect { String k, String v ->
        URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
      }.join("&")
    )
  }

}
