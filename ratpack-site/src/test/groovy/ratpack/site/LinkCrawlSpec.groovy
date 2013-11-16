/*
 * Copyright 2013 the original author or authors.
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

import ratpack.groovy.test.LocalScriptApplicationUnderTest
import ratpack.site.crawl.Crawler
import ratpack.site.crawl.PrettyPrintCollection
import ratpack.test.ApplicationUnderTest
import spock.lang.Specification

class LinkCrawlSpec extends Specification {

  def "site has no bad links"() {
    given:
    ApplicationUnderTest aut = new LocalScriptApplicationUnderTest()

    def crawler = new Crawler(aut.address.toString()) {
      boolean shouldUseHeadRequest(Link url) {
        return url.uri.host != "bintray.com" && super.shouldUseHeadRequest(url)
      }

      void pushPageLinks(Response response) {
        response.document?.select("body a")?.each {
          def href = it.attr("href")
          if (href) {
            push href
          }
        }
      }
    }

    when:
    crawler.crawl()
    def errored = new PrettyPrintCollection(crawler.visited.findAll { it.errors.size() > 0 })

    then:
    errored.empty

    cleanup:
    aut.stop()
  }
}
