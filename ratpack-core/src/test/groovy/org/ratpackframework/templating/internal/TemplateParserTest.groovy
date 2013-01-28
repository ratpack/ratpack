package org.ratpackframework.templating.internal

import org.vertx.java.core.buffer.Buffer
import spock.lang.Specification

class TemplateParserTest extends Specification {

  private final TemplateParser parser = new TemplateParser()

  String parse(String source) {
    Buffer sourceBuffer = new Buffer(source)
    Buffer scriptBuffer = new Buffer(source.size())
    parser.parse(sourceBuffer, scriptBuffer)
    scriptBuffer.toString()
  }

  def "encoding"() {
    expect:
    parse("abc") == '$o();str("""abc""");$c();'
    parse("aéc") == '$o();str("""aéc""");$c();'
    parse("a\u1234c") == '$o();str("""a\u1234c""");$c();'
  }

  def "gstrings"() {
    expect:
    parse("a\${'b'}c") == '$o();str("""a${\'b\'}c""");$c();'
  }

  def "code blocks"() {
    expect:
    parse("a<% b %>c") == '$o();str("""a""");$c(); b ;$o();str("""c""");$c();'
  }

}
