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
import com.fasterxml.jackson.databind.ObjectReader
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import groovy.transform.CompileStatic

import java.util.concurrent.TimeUnit

@CompileStatic
@javax.inject.Singleton
class GitHubApi {

  private final String api
  private final String authToken
  private final int cacheMins
  private final ObjectReader objectReader

  private final LoadingCache<String, JsonNode> cache = CacheBuilder.newBuilder().
    initialCapacity(30).
    expireAfterWrite(cacheMins, TimeUnit.MINUTES).
    build(new CacheLoader<String, JsonNode>() {
      @Override
      JsonNode load(String key) throws Exception {
        def body = new URL(key).text
        objectReader.readTree(body)
      }
    })

  GitHubApi(String api, String authToken, int cacheMins, ObjectReader objectReader) {
    this.api = api
    this.authToken = authToken
    this.cacheMins = cacheMins
    this.objectReader = objectReader
  }

  private JsonNode get(String path, Map<String, String> params) {
    cache.get(api + path + toQueryString(params))
  }

  JsonNode milestones(Map<String, String> params) {
    get("repos/ratpack/ratpack/milestones", params)
  }

  JsonNode issues(Map<String, String> params) {
    get("repos/ratpack/ratpack/issues", params)
  }

  void invalidateCache() {
    cache.invalidateAll()
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
