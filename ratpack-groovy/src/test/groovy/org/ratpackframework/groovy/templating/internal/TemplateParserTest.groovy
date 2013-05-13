package org.ratpackframework.groovy.templating.internal

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import org.ratpackframework.util.internal.IoUtils
import spock.lang.Specification

class TemplateParserTest extends Specification {

  private final TemplateParser parser = new TemplateParser()

  String parse(String source) {
    ByteBuf sourceBuffer = IoUtils.utf8Buffer(source)
    ByteBuf scriptBuffer = Unpooled.buffer(source.length())
    parser.parse(sourceBuffer, scriptBuffer)
    scriptBuffer.toString(CharsetUtil.UTF_8)
  }

  def "encoding"() {
    expect:
    parse("abc") == '$(\n"""abc"""\n);'
    parse("aéc") == '$(\n"""aéc"""\n);'
    parse("a\u1234c") == '$(\n"""a\u1234c"""\n);'
  }

  def "gstrings"() {
    expect:
    parse("a\${'b'}c") == '$(\n"""a${\'b\'}c"""\n);'
  }

  def "code blocks"() {
    expect:
    parse("a<% b %>c") == '$(\n"""a"""\n);\n b \n;$(\n"""c"""\n);'
  }

}
