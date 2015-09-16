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
import io.netty.util.CharsetUtil
import spock.lang.Specification

class TemplateParserSpec extends Specification {

  private final TextTemplateParser parser = new TextTemplateParser()

  String parse(String source) {
    ByteBuf sourceBuffer = Unpooled.copiedBuffer(source, CharsetUtil.UTF_8)
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
