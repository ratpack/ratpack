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

package ratpack.groovy.template.internal

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.util.CharsetUtil
import ratpack.groovy.script.internal.ScriptEngine
import spock.lang.Specification

class TemplateCompilerSpec extends Specification {

  def compiler = new TextTemplateCompiler(new ScriptEngine<DefaultTextTemplateScript>(getClass().classLoader, true, DefaultTextTemplateScript), true, UnpooledByteBufAllocator.DEFAULT)

  CompiledTextTemplate compile(String source) {
    compiler.compile(Unpooled.copiedBuffer(source, CharsetUtil.UTF_8), "test")
  }

  class StubNestedRenderer implements NestedRenderer {
    ByteBuf buffer

    @Override
    void render(String templateName, Map<String, ?> model) {
      buffer.writeBytes("render:${[templateName: templateName, model: model]}".getBytes(CharsetUtil.UTF_8))
    }
  }

  String exec(String script) {
    ByteBuf buffer = Unpooled.buffer(script.size())
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
