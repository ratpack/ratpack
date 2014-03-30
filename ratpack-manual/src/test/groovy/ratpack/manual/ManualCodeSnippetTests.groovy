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

import ratpack.manual.snippets.CodeSnippetTestCase
import ratpack.manual.snippets.CodeSnippetTests
import ratpack.manual.snippets.ManualSnippetExtractor
import ratpack.manual.snippets.fixtures.DoNothingSnippetFixture
import ratpack.manual.snippets.fixtures.GradleFixture
import ratpack.manual.snippets.fixtures.GroovyChainDslFixture
import ratpack.manual.snippets.fixtures.GroovyHandlersFixture
import ratpack.manual.snippets.fixtures.GroovyRatpackDslFixture
import ratpack.manual.snippets.fixtures.SnippetFixture

class ManualCodeSnippetTests extends CodeSnippetTestCase {

  public static final LinkedHashMap<String, SnippetFixture> FIXTURES = [
    "language-groovy groovy-chain-dsl": new GroovyChainDslFixture(),
    "language-groovy groovy-ratpack"  : new GroovyRatpackDslFixture(),
    "language-groovy groovy-handlers" : new GroovyHandlersFixture(),
    "language-groovy gradle"          : new GradleFixture(),
    "language-groovy tested"          : new DoNothingSnippetFixture()
  ]

  @Override
  protected void addTests(CodeSnippetTests tests) {
    File cwd = new File(System.getProperty("user.dir"))
    File root
    if (new File(cwd, "ratpack-manual.gradle").exists()) {
      root = cwd.parentFile
    } else {
      root = cwd
    }

    def content = new File(root, "ratpack-manual/src/content/chapters")

    FIXTURES.each { selector, fixture ->
      ManualSnippetExtractor.extract(content, selector, fixture).each {
        tests.add(it)
      }
    }
  }

}
