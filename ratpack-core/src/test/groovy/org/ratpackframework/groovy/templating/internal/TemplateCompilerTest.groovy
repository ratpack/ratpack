package org.ratpackframework.groovy.templating.internal

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import org.ratpackframework.io.IoUtils
import org.ratpackframework.groovy.ScriptEngine
import spock.lang.Specification

class TemplateCompilerTest extends Specification {

  def compiler = new TemplateCompiler(new ScriptEngine<TemplateScript>(getClass().classLoader, true, TemplateScript), true)

  CompiledTemplate compile(String source) {
    compiler.compile(IoUtils.utf8Buffer(source), "test")
  }

  class StubNestedRenderer implements NestedRenderer {
    ChannelBuffer buffer

    @Override
    void render(String templateName, Map<String, ?> model) {
      buffer.writeBytes(IoUtils.utf8Bytes("render:${[templateName: templateName, model: model]}"))
    }
  }

  String exec(String script) {
    ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(script.size())
    compile(script).execute([:], buffer, new StubNestedRenderer(buffer: buffer))
    buffer.toString(CharsetUtil.UTF_8)
  }

  def "compile"() {
    expect:
    exec("abc") == "abc"
    exec("1\${'2'}3") == "123"
    exec("1<% %>3") == "13"
  }

  def "rendering"() {
    expect:
    exec("a-<% render 'foo' %>-c") == "a-render:[templateName:foo, model:[:]]-c"
  }

}
