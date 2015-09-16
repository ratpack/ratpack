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

import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.node.ArrayNode
import groovy.transform.CompileStatic
import ratpack.exec.Promise
import ratpack.http.client.HttpClient
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
import rx.Observable
import rx.Subscriber

import static ratpack.rx.RxRatpack.observe

@CompileStatic
class GithubRequester {

  private final ObjectReader objectReader
  private final HttpClient httpClient

  GithubRequester(ObjectReader objectReader, HttpClient httpClient) {
    this.httpClient = httpClient
    this.objectReader = objectReader
  }

  Promise<ArrayNode> request(String url) {
    Observable.create({ Subscriber<ArrayNode> it ->
      getPage(httpClient, url, it)
    } as Observable.OnSubscribe<ArrayNode>).reduce { ArrayNode a, ArrayNode b ->
      a.addAll(b)
    }.promiseSingle()
  }

  private void getPage(HttpClient httpClient, String url, Subscriber<ArrayNode> pagingSubscription) {
    def promise = httpClient.get(new URI(url)) { RequestSpec it ->
      it.headers.set("User-Agent", "http://www.ratpack.io")
    }

    observe(promise).subscribe { ReceivedResponse response ->
      def node = objectReader.readTree(response.body.text)
      if (node instanceof ArrayNode) {
        pagingSubscription.onNext((ArrayNode) node)
        def linkHeaderValue = response.headers.get("Link")
        def next = getNextUrl(linkHeaderValue)
        if (next) {
          getPage(httpClient, next, pagingSubscription)
        } else {
          pagingSubscription.onCompleted()
        }
      } else {
        pagingSubscription.onError(new RuntimeException("Not an array response from $url (was ${node.getClass()}): \n$response.body.text}"))
      }
    }
  }

  private static String getNextUrl(String linkHeaderValue) {
    if (linkHeaderValue == null) {
      return null
    }

    String[] links = linkHeaderValue.split(",")
    for (String link : links) {
      String[] segments = link.split(";")
      if (segments.length < 2) {
        continue
      }

      String linkPart = segments[0].trim()
      if (!linkPart.startsWith("<") || !linkPart.endsWith(">")) {
        continue
      }
      linkPart = linkPart.substring(1, linkPart.length() - 1)

      for (int i = 1; i < segments.length; i++) {
        String[] rel = segments[i].trim().split("=")
        if (rel.length < 2 || !rel[0].equals("rel")) {
          continue
        }

        String relValue = rel[1]
        if (relValue.startsWith("\"") && relValue.endsWith("\"")) {
          relValue = relValue.substring(1, relValue.length() - 1)
        }

        if (relValue.equals("next")) {
          return linkPart
        }
      }
    }

    null
  }

}
