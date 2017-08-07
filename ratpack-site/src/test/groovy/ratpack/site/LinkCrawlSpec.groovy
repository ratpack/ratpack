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

import groovy.util.logging.Slf4j
import ratpack.site.crawl.Crawler
import ratpack.site.crawl.PrettyPrintCollection
import ratpack.util.RatpackVersion
import spock.lang.Specification

@Slf4j
class LinkCrawlSpec extends Specification {

  def "site has no bad links"() {
    given:
    def aut = new RatpackSiteUnderTest()

    def allowBroken = [
      "http://www.pac4j.org",
      "http://www.sparkjava.com",
      "http://www.astigmatic.com",
      "https://drone.io",
      "http://search.maven.org",
      "http://geekfairy.co.uk",
      "http://lea.verou.me/",
      "http://www.yourkit.com",
      "http://thecodingdesigner.com",
      "https://kotlinlang.org",
      "http://www.slf4j.org",
      "https://travis-ci.org",
      "http://www.gebish.org",
      "https://github.com/alkemist"
    ]

    def crawler = new Crawler(aut.address.toString()) {
      boolean shouldUseHeadRequest(Link url) {
        return url.uri.host != "bintray.com" && super.shouldUseHeadRequest(url)
      }

      @Override
      boolean isCrawlable(Link link) {
        if (link.uri.path.startsWith("/manual") && !link.uri.path.startsWith("/manual/${RatpackVersion.version - "-SNAPSHOT"}")) {
          false
        } else {
          super.isCrawlable(link)
        }
      }

      List<String> findPageLinks(Response response) {
        def document = response.document
        document == null ? [] : document.select("body a").collect {
          it.attr("href")
        }.findAll {
          it
        }
      }

      @Override
      void addPageErrors(Link link, Response response) {
        response.document?.text()?.findAll(~$/\[.+]\(.+\)/$)?.each {
          link.errors << new BadMarkdownLinkSyntax(it)
        }
        super.addPageErrors(link, response)
      }
    }

    when:
    def visited = crawler.crawl()
    def broken = visited.findAll { it.errors.size() > 0 }
    def brokenByLevel = broken.groupBy { link -> allowBroken.any { link.uri.toString().startsWith(it) } ? "warn" : "error" }
    def errored = new PrettyPrintCollection(brokenByLevel["error"] ?: [])
    def warned = new PrettyPrintCollection(brokenByLevel["warn"] ?: [])
    if (!warned.empty) {
      log.warn "${warned}"
    }

    then:
    errored.empty

    cleanup:
    aut.stop()
  }

  private static class BadMarkdownLinkSyntax extends Crawler.PageError {
    final String link

    BadMarkdownLinkSyntax(String link) {
      this.link = link
    }

    @Override
    String toString() {
      "Bad markdown link: $link"
    }
  }
}
