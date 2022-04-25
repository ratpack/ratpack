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
import ratpack.rx2.RxRatpack
import ratpack.manual.snippets.executer.GradleSnippetExecuter
import ratpack.test.docs.CodeSnippetTestCase
import ratpack.test.docs.SnippetExtractor
import ratpack.test.docs.SnippetFixture
import ratpack.test.docs.executer.GroovySnippetExecuter
import ratpack.test.docs.executer.JavaSnippetExecuter
import ratpack.test.docs.SnippetExecuter
import ratpack.manual.snippets.extractor.ManualSnippetExtractor
import ratpack.test.docs.fixture.GroovyScriptFixture
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
  File getSourceDirectory() {
    File cwd = new File(StandardSystemProperty.USER_DIR.value())
    File root
    if (new File(cwd, "ratpack-manual.gradle").exists()) {
      root = cwd.parentFile
    } else {
      root = cwd
    }

    return new File(root, "ratpack-manual/src/content/chapters")
  }

  @Override
  String getIncludePath() {
    return "**/*.md"
  }

  @Override
  SnippetExtractor getExtractor() {
    return new ManualSnippetExtractor()
  }

  @Override
  Map<String, SnippetExecuter> getCssExecutorMapping() {
    return FIXTURES
  }

}
