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



package ratpack.site.crawl

import groovy.transform.CompileStatic
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ratpack.http.MediaType
import ratpack.http.internal.DefaultMediaType

import javax.net.ssl.*
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

@CompileStatic
abstract class Crawler {

  final int retryLimit = 3
  final int retryWaitMillis = 1000

  final String startingUrl

  protected String currentUrl
  protected List<Link> toVisit = []

  final Set<Link> visited = new LinkedHashSet()

  protected Response lastResponse

  Crawler(String startingUrl) {
    this.startingUrl = startingUrl
  }

  boolean isCrawlable(Link link) {
    isUnderStartUrl(link) && !link.uri.fragment
  }

  def boolean isUnderStartUrl(Link link) {
    link.uri.toString().startsWith(startingUrl)
  }

  boolean shouldUseHeadRequest(Link url) {
    !isUnderStartUrl(url)
  }

  Collection<String> getDownloadableExtensions() {
    ["pdf", "zip", "jar"]
  }

  boolean isDownload(Link link) {
    downloadableExtensions.any { link.uri.toString().endsWith(".$it") }
  }

  boolean shouldVisit(Link link) {
    true
  }

  boolean isExpectedErroredLink(Link erroredLink) {
    false
  }

  abstract void pushPageLinks(Response response)

  void addPageErrors(Link link, Response response) {
    if (response.statusCode != 200) {
      link.errors << new StatusCodeError(response.statusCode)
    }

    def fragment = link.uri.fragment
    if (response.document && fragment) {
      if (!response.document.select("a").any { Element it -> it.attr("name") == fragment } && response.document.getElementById(fragment) == null) {
        link.errors << new BadFragmentError(link.uri.fragment)
      }
    }
  }

  void crawl() {
    toVisit << new Link(startingUrl)

    def next = nextToVisit
    while (next != null) {
      visit(next)
      next = nextToVisit
    }
  }

  protected visit(Link link) {
    currentUrl = link.uri.toString()
    println "visiting: $link.uri (attempts: $link.attemptCount)"
    if (link.lastAttemptAt >= 0) {
      def waitMillis = retryWaitMillis - (System.currentTimeMillis() - link.lastAttemptAt)
      if (waitMillis > 0) {
        sleep waitMillis
      }
    }

    link.attempt()

    if (!isDownload(link) && isCrawlable(link)) {
      visitCrawlable link
    } else {
      visitNonCrawlable link
    }
    if (link.errors) {
      if (link.attemptCount < retryLimit) {
        println "attemptCount = $link.attemptCount, putting back on queue"
        link.errors.clear()
        toVisit << link
      } else {
        println "giving up on this link"
      }
    }
  }

  protected visitCrawlable(Link link) {
    lastResponse = new Response(link.uri, openUrlConnection(link.uri))

    addPageErrors(link, lastResponse)

    if (link.errors) {
      println "has errors: $link.errors"
    } else {
      if (lastResponse.contentType.html) {
        pushPageLinks(lastResponse)
      } else {
        println "not finding links, contentType == $lastResponse.contentType"
      }
    }
  }

  protected visitNonCrawlable(Link link) {
    def connection = openUrlConnection(link.uri)
    def method = shouldUseHeadRequest(link) ? "HEAD" : "GET"
    println "making direct $method request on non crawlable url $link.uri"
    connection.requestMethod = method

    try {
      while (connection.responseCode > 300 && connection.responseCode < 400) {
        def redirectTo = connection.getURL().toURI().resolve(connection.getHeaderField("location")).toString()
        println "following redirect ($connection.responseCode) to $redirectTo"
        link = new Link(redirectTo)
        connection = openUrlConnection(link.uri)
      }

      addPageErrors(link, new Response(link.uri, connection))
    } catch (IOException e) {
      println "$e making request to $connection.URL"
      link.errors << new ExceptionError(e)
    }
  }

  protected HttpURLConnection openUrlConnection(URI uri) {
    HttpURLConnection connection = uri.toURL().openConnection() as HttpURLConnection
    connection.instanceFollowRedirects = false

    connection.connectTimeout = 60000
    connection.readTimeout = 60000

    if (connection instanceof HttpsURLConnection) {
      def https = connection as HttpsURLConnection
      https.setHostnameVerifier(new HostnameVerifier() {
        @Override
        boolean verify(String s, SSLSession sslSession) {
          return true
        }
      })

      def trustManager = new X509TrustManager() {
        @Override
        void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0];
        }
      }

      def sc = SSLContext.getInstance("SSL")
      sc.init([] as KeyManager[], [trustManager] as TrustManager[], new SecureRandom())
      https.setSSLSocketFactory(sc.socketFactory)
    }
    connection
  }

  protected Link getNextToVisit() {
    if (toVisit) {
      def next = toVisit.remove(0)
      visited << next
      next
    } else {
      null
    }
  }

  protected void push(String... urls) {
    urls.each { String url ->

      URI uri = new URI(url.replaceAll("\\s", "%20"))
      def href = uri.isAbsolute() ? uri.toString() : new URI(currentUrl).resolve(uri).toString()
      if (href == null) {
        return
      }

      href = NormalizeURL.normalize(href)

      println "inspecting url $url (normalized: $href) on $currentUrl"

      if (isHttpUrl(href)) {
        assert href.startsWith('http')
        Link link = new Link(href)
        link.referrers << lastResponse.uri.toString()
        if (shouldVisit(link)) {
          def existingLink = findExistingLinkObject(link)
          if (existingLink) {
            existingLink.referrers << currentUrl
            println "already visited $link or is in toVisit list"
          } else {
            println "adding $link to list"
            toVisit << link
          }
        } else {
          println "not going to visit $href"
        }
      } else {
        println "ignoring $href as it's not a http url"
      }
    }
  }

  boolean isHttpUrl(String url) {
    url==~~/^https?:\/.+/
  }

  Link findExistingLinkObject(Link link) {
    visited.find { Link it -> link.uri == it.uri } ?: toVisit.find { Link it -> link.uri == it.uri }
  }

  class Response {
    final int statusCode
    final URI uri
    final MediaType contentType
    final Document document

    protected final HttpURLConnection connection

    Response(URI uri, HttpURLConnection connection) {
      this.uri = uri
      this.connection = connection

      // Force the request
      statusCode = connection.responseCode
      contentType = DefaultMediaType.get(connection.getHeaderField("Content-Type"))

      if (connection.requestMethod == "GET" && contentType.html) {
        def stream = statusCode >= 400 ? connection.errorStream : connection.inputStream
        document = Jsoup.parse(stream, contentType.charset, uri.toString())
      } else {
        document = null
      }
    }
  }

  static abstract class PageError {}

  static class StatusCodeError extends PageError {
    final int code

    StatusCodeError(int code) { this.code = code }

    String toString() { "HTTP status: $code" }
  }

  static class BadFragmentError extends PageError {
    final String fragment

    BadFragmentError(String fragment) { this.fragment = fragment }

    String toString() { "Bad fragment: $fragment" }
  }

  static class ExceptionError extends PageError {
    final Exception exception

    ExceptionError(Exception exception) { this.exception = exception }

    String toString() { "Exception: $exception" }
  }

  static class Link {
    final URI uri

    final Set<String> referrers = []
    final List<PageError> errors = []

    int attemptCount = 0
    long lastAttemptAt = 0

    Link(String url) {
      this.uri = new URI(url)
    }

    String toString() {
      "$uri (referrers: $referrers, errors: $errors, attemps: $attemptCount)"
    }

    void attempt() {
      ++attemptCount
      lastAttemptAt = System.currentTimeMillis()
    }
  }
}
