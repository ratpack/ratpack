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

import ratpack.site.crawl.Crawler
import ratpack.site.crawl.PrettyPrintCollection
import spock.lang.Specification

class LinkCrawlSpec extends Specification {

  def "site has no bad links"() {
    given:
    def aut = new RatpackSiteUnderTest()
    aut.mockGithubData()

    def dontCrawl = ["/manual/current", "/manual/0.9.0"]

    def crawler = new Crawler(aut.address.toString()) {
      boolean shouldUseHeadRequest(Link url) {
        return url.uri.host != "bintray.com" && super.shouldUseHeadRequest(url)
      }

      @Override
      boolean isCrawlable(Link link) {
        if (dontCrawl.any { link.uri.path.startsWith(it) }) {
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
        response.document.text().findAll(~$/\[.+?]\(.+?\(.+?\)\)/$).each {
          link.errors << new BadMarkdownLinkSyntax(it)
        }
        super.addPageErrors(link, response)
      }
    }

    when:
    def visited = crawler.crawl()
    def errored = new PrettyPrintCollection(visited.findAll { it.errors.size() > 0 })

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
