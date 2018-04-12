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

package ratpack.manual

import com.google.common.base.StandardSystemProperty
import ratpack.rx.RxRatpack
import ratpack.test.internal.snippets.CodeSnippetTestCase
import ratpack.test.internal.snippets.CodeSnippetTests
import ratpack.test.internal.snippets.executer.GradleSnippetExecuter
import ratpack.test.internal.snippets.executer.GroovySnippetExecuter
import ratpack.test.internal.snippets.executer.JavaSnippetExecuter
import ratpack.test.internal.snippets.executer.SnippetExecuter
import ratpack.manual.snippets.extractor.ManualSnippetExtractor
import ratpack.test.internal.snippets.fixture.*

class ManualCodeSnippetTests extends CodeSnippetTestCase {

  static {
    RxRatpack.initialize()
  }

  public static final LinkedHashMap<String, SnippetExecuter> FIXTURES = [
    "language-groovy groovy-chain-dsl": new GroovySnippetExecuter(true, new GroovyChainDslFixture()),
    "language-groovy groovy-ratpack"  : new GroovySnippetExecuter(true, new GroovyRatpackDslNoRunFixture()),
    "language-groovy groovy-handlers" : new GroovySnippetExecuter(true, new GroovyHandlersFixture()),
    "language-groovy gradle"          : new GradleSnippetExecuter(new SnippetFixture()),
    "language-groovy tested"          : new GroovySnippetExecuter(true, new GroovyScriptFixture()),
    "language-java"                   : new JavaSnippetExecuter(new SnippetFixture()),
    "language-java hello-world"       : new HelloWorldAppSnippetExecuter(new JavaSnippetExecuter(new SnippetFixture())),
    "language-groovy hello-world"     : new HelloWorldAppSnippetExecuter(new GroovySnippetExecuter(true, new GroovyScriptRatpackDslFixture())),
    "language-groovy hello-world-grab": new HelloWorldAppSnippetExecuter(new GroovySnippetExecuter(true, new GroovyScriptRatpackDslFixture() {
      @Override
      String transform(String text) {
        return text.readLines()[4..-1].join("\n")
      }
    }))
  ]

  @Override
  protected void addTests(CodeSnippetTests tests) {
    File cwd = new File(StandardSystemProperty.USER_DIR.value())
    File root
    if (new File(cwd, "ratpack-manual.gradle").exists()) {
      root = cwd.parentFile
    } else {
      root = cwd
    }

    def content = new File(root, "ratpack-manual/src/content/chapters")

    FIXTURES.each { selector, executer ->
      ManualSnippetExtractor.extract(content, selector, executer).each {
        tests.add(it)
      }
    }
  }

}
