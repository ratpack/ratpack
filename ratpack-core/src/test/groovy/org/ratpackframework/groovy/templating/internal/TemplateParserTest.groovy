package org.ratpackframework.groovy.templating.internal

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import org.ratpackframework.io.IoUtils
import spock.lang.Specification

class TemplateParserTest extends Specification {

  private final TemplateParser parser = new TemplateParser()

  String parse(String source) {
    ChannelBuffer sourceBuffer = IoUtils.utf8Buffer(source)
    ChannelBuffer scriptBuffer = ChannelBuffers.dynamicBuffer(source.length())
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
