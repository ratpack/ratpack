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
    parse("abc") == '$o();$s("""abc""");$c();'
    parse("aéc") == '$o();$s("""aéc""");$c();'
    parse("a\u1234c") == '$o();$s("""a\u1234c""");$c();'
  }

  def "gstrings"() {
    expect:
    parse("a\${'b'}c") == '$o();$s("""a${\'b\'}c""");$c();'
  }

  def "code blocks"() {
    expect:
    parse("a<% b %>c") == '$o();$s("""a""");$c(); b ;$o();$s("""c""");$c();'
  }

}
