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
import ratpack.manual.snippets.extractor.JavadocSnippetExtractor
import ratpack.manual.snippets.fixture.*

class JavadocCodeSnippetTests extends CodeSnippetTestCase {

  public static final LinkedHashMap<String, SnippetFixture> FIXTURES = [
    "tested"            : new GroovyScriptFixture(),
    "java-chain-dsl"    : new JavaChainDslFixture(),
    "groovy-chain-dsl"  : new GroovyChainDslFixture(),
    "groovy-ratpack-dsl": new GroovyRatpackDslFixture(),
    "java"              : new JavaExampleClassFixture(),
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

    root.eachDirMatch(~/ratpack-.+/) {
      def mainSrc = new File(it, "src/main")
      if (mainSrc.exists()) {
        FIXTURES.each { selector, snippetFixture ->
          JavadocSnippetExtractor.extract(mainSrc, "**/*.java", selector, snippetFixture).each {
            tests.add(it)
          }
        }
      }
    }
  }
}
