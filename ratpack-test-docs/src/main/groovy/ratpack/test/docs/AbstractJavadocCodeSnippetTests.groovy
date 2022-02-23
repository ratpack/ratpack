/*
 * Copyright 2022 the original author or authors.
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

package ratpack.test.docs

import ratpack.test.docs.extractor.JavadocSnippetExtractor

abstract class AbstractJavadocCodeSnippetTests extends CodeSnippetTestCase {

  abstract File getSourceDirectory()

  abstract Map<String, SnippetExecuter> getCssExecutorMapping()

  @Override
  protected Collection<TestCodeSnippet> registerTests() {
    def mainSrc = getSourceDirectory()
    if (mainSrc.exists()) {
      getCssExecutorMapping().collectMany { selector, executer ->
        JavadocSnippetExtractor.extract(mainSrc, "**/*.java", selector, executer)
      }
    }
  }
}
