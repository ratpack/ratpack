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
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

import static org.codehaus.groovy.runtime.StackTraceUtils.deepSanitize

@SuppressWarnings("GrMethodMayBeStatic")
@CompileStatic
abstract class Crawler {

  final int numThreads = Runtime.getRuntime().availableProcessors() * 4
  final int retryLimit = 3
  final int retryWaitMillis = 200
  final int timeoutMins = 10

  final String startingUrl

  protected final AtomicInteger counter = new AtomicInteger(0)
  protected final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(numThreads)

  protected final ConcurrentMap<String, Link> seen = new ConcurrentHashMap<>()
  protected final Queue<Link> visited = new ConcurrentLinkedQueue<>()

  Crawler(String startingUrl) {
    this.startingUrl = startingUrl
  }

  abstract List<String> findPageLinks(Response response)

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

  void addPageErrors(Link link, Response response) {
    if (response.statusCode == 404) {
      println "404: $link.uri"
      link.errors << new StatusCodeError(response.statusCode)
    }

    def fragment = link.uri.fragment
    if (response.document && fragment) {
      if (!response.document.select("a").any { Element it -> it.attr("name") == fragment } && response.document.getElementById(fragment) == null) {
        println "bad fragment: $link.uri"
        link.errors << new BadFragmentError(link.uri.fragment)
      }
    }
  }

  List<Link> crawl() {
    def link = new Link(startingUrl)
    seen.put(startingUrl, new Link(startingUrl))
    executorService.execute(toVisit(link))

    long startAt = System.currentTimeMillis()
    long stopAt = startAt + (1000 * 60 * timeoutMins)
    while (counter.get() > 0 && System.currentTimeMillis() < stopAt) {
      sleep 100
    }

    if (counter.get() == 0) {
      new ArrayList<>(visited)
    } else {
      throw new RuntimeException("timeout")
    }
  }

  Runnable toVisit(Link link) {
    counter.incrementAndGet()
    return {
      try {
        visit(link)
      } catch (e) {
        link.errors << new ExceptionError(e)
      } finally {
        counter.getAndDecrement()
      }
    }
  }

  protected visit(Link link) {
    link.attempt()

    if (!isDownload(link) && isCrawlable(link)) {
      visitCrawlable link
    } else {
      visitNonCrawlable link
    }
    if (link.errors) {
      if (link.attemptCount < retryLimit) {
        link.errors.clear()
        executorService.schedule(toVisit(link), retryWaitMillis, TimeUnit.MILLISECONDS)
      } else {
        visited << link
      }
    }
  }

  protected visitCrawlable(Link link) {
    Response lastResponse = new Response(link.uri, openUrlConnection(link.uri))

    addPageErrors(link, lastResponse)

    if (!link.errors && lastResponse.contentType.html) {
      findPageLinks(lastResponse).each { String it ->
        def newLink = toLink(link.uri, it)
        if (newLink) {
          executorService.execute(toVisit(newLink))
        }
      }
    }
  }

  protected visitNonCrawlable(Link link) {
    def connection = openUrlConnection(link.uri)
    def method = shouldUseHeadRequest(link) ? "HEAD" : "GET"
    connection.requestMethod = method

    try {
      while (connection.responseCode > 300 && connection.responseCode < 400) {
        def redirectTo = connection.getURL().toURI().resolve(connection.getHeaderField("location")).toString()
        link = new Link(redirectTo)
        connection = openUrlConnection(link.uri)
      }

      addPageErrors(link, new Response(link.uri, connection))
    } catch (IOException e) {
      println "error: $link.uri - $e"
      link.errors << new ExceptionError(e)
    }
  }

  protected HttpURLConnection openUrlConnection(URI uri) {
    println "request: $uri"
    HttpURLConnection connection = uri.toURL().openConnection() as HttpURLConnection
    connection.instanceFollowRedirects = false

    connection.connectTimeout = 60000
    connection.readTimeout = 60000

    if (connection instanceof HttpsURLConnection) {
      def https = connection as HttpsURLConnection

      def trustManager = new X509TrustManager() {
        @Override
        void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0]
        }
      }

      def sc = SSLContext.getDefault()
      sc.init([] as KeyManager[], [trustManager] as TrustManager[], new SecureRandom())
      https.setSSLSocketFactory(new DelegatingSSLSocketFactory(sc.socketFactory))
    }
    connection
  }

  protected Link toLink(URI currentUrl, String url) {
    URI uri = new URI(url.replaceAll("\\s", "%20"))
    def href = uri.isAbsolute() ? uri.toString() : currentUrl.resolve(uri).toString()
    href = NormalizeURL.normalize(href)
    if (isHttpUrl(href)) {
      assert href.startsWith('http')
      def newLink = new Link(href)
      def existing = seen.putIfAbsent(href, newLink)
      def link = existing ?: newLink
      link.referrers.add(currentUrl.toString())

      return existing ? null : newLink
    }

    null
  }

  boolean isHttpUrl(String url) {
    url ==~ ~/^https?:\/.+/
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
    final Throwable exception

    ExceptionError(Throwable exception) { this.exception = deepSanitize(exception) }

    String toString() {
      def stringWriter = new StringWriter()
      def printWriter = new PrintWriter(stringWriter)
      printWriter.println(exception.message)
      exception.printStackTrace(printWriter)
      stringWriter.toString()
    }
  }

  static class Link {
    final URI uri

    final Set<String> referrers = new HashSet().asSynchronized()
    final List<PageError> errors = [].asSynchronized()

    int attemptCount = 0

    Link(String url) {
      this.uri = new URI(url)
    }

    String toString() {
      "$uri (referrers: $referrers, errors: $errors, attempts: $attemptCount)"
    }

    void attempt() {
      ++attemptCount
    }

    boolean equals(o) {
      if (this.is(o)) {
        return true
      }
      if (getClass() != o.class) {
        return false
      }

      Link link = (Link) o

      uri == link.uri
    }

    int hashCode() {
      return uri.hashCode()
    }
  }
}
