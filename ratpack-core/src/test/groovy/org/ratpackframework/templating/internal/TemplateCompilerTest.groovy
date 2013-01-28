package org.ratpackframework.templating.internal

import org.ratpackframework.script.internal.ScriptEngine
import org.vertx.java.core.buffer.Buffer
import spock.lang.Specification

class TemplateCompilerTest extends Specification {

  def compiler = new TemplateCompiler(new ScriptEngine<TemplateScript>(getClass().classLoader, true, TemplateScript), true)

  CompiledTemplate compile(String source) {
    compiler.compile(new Buffer(source), "test")
  }

  class StubNestedRenderer implements NestedRenderer {
    @Override
    NestedTemplate render(String templateName, Map<String, ?> model) {
      new NestedTemplate() {
        @Override
        String toString() {
          "render:${[templateName: templateName, model: model]}"
        }
      }
    }
  }

  def renderer = new StubNestedRenderer()

  def "compile"() {
    expect:
    compile("abc").execute([:], renderer).parts == ["abc"]
    compile("1\${'2'}3").execute([:], renderer).parts == ["123"]
    compile("1<% %>3").execute([:], renderer).parts == ["1", "3"]
  }

  def "rendering"() {
    expect:
    compile("a<%= render 'foo' %>c").execute([:], renderer).parts*.toString() == ["a", "render:[templateName:foo, model:[:]]", "c"]
  }

}
