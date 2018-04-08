/*
 * Copyright 2018 the original author or authors.
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

package ratpack.test.internal.snippets

import com.google.common.base.StandardSystemProperty
import ratpack.test.internal.snippets.executer.GroovySnippetExecuter
import ratpack.test.internal.snippets.executer.JavaSnippetExecuter
import ratpack.test.internal.snippets.executer.SnippetExecuter
import ratpack.test.internal.snippets.extractor.JavadocSnippetExtractor
import ratpack.test.internal.snippets.fixture.GroovyChainDslFixture
import ratpack.test.internal.snippets.fixture.GroovyRatpackDslNoRunFixture
import ratpack.test.internal.snippets.fixture.GroovyScriptFixture
import ratpack.test.internal.snippets.fixture.JavaChainDslFixture
import ratpack.test.internal.snippets.fixture.SnippetFixture

abstract class AbstractJavadocCodeSnippetTests extends CodeSnippetTestCase {

  public static final LinkedHashMap<String, SnippetExecuter> FIXTURES = [
    "tested"            : new GroovySnippetExecuter(true, new GroovyScriptFixture()),
    "tested-dynamic"    : new GroovySnippetExecuter(false, new GroovyScriptFixture()),
    "java-chain-dsl"    : new JavaSnippetExecuter(new JavaChainDslFixture()),
    "groovy-chain-dsl"  : new GroovySnippetExecuter(true, new GroovyChainDslFixture()),
    "groovy-ratpack-dsl": new GroovySnippetExecuter(true, new GroovyRatpackDslNoRunFixture()),
    "java"              : new JavaSnippetExecuter(new SnippetFixture()),
    "java-args"         : new JavaSnippetExecuter(new SnippetFixture(), "thing.name=foo"),
  ]

  abstract String getProjectName()

  @Override
  protected void addTests(CodeSnippetTests tests) {
    File cwd = new File(StandardSystemProperty.USER_DIR.value())
    File root
    if (new File(cwd, "${projectName}.gradle").exists()) {
      root = cwd
    } else {
      root = new File(cwd, projectName)
    }

    def mainSrc = new File(root, "src/main")
    if (mainSrc.exists()) {
      FIXTURES.each { selector, executer ->
        JavadocSnippetExtractor.extract(mainSrc, "**/*.java", selector, executer).each {
          tests.add(it)
        }
      }
    }
  }
}
