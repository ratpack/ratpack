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

package org.ratpackframework.manual

import org.ratpackframework.manual.snippets.CodeSnippetTestCase
import org.ratpackframework.manual.snippets.CodeSnippetTests
import org.ratpackframework.manual.snippets.fixtures.DoNothingSnippetFixture
import org.ratpackframework.manual.snippets.JavadocSnippetExtractor
import org.ratpackframework.manual.snippets.SnippetExecuter
import org.ratpackframework.manual.snippets.fixtures.GroovyChainDslFixture
import org.ratpackframework.manual.snippets.fixtures.GroovyRatpackDslFixture
import org.ratpackframework.manual.snippets.fixtures.SnippetFixture

class JavadocCodeSnippetTests extends CodeSnippetTestCase {

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
        [
          "tested": new DoNothingSnippetFixture(),
          "groovy-chain-dsl": new GroovyChainDslFixture(),
          "groovy-ratpack-dsl": new GroovyRatpackDslFixture()
        ].each { selector,  snippetFixture ->
          JavadocSnippetExtractor.extract(mainSrc, "**/*.java", selector, snippetFixture).each {
            tests.add(it)
          }
        }
      }
    }
  }
}
