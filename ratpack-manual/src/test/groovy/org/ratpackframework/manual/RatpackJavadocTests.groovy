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

import org.ratpackframework.manual.fixture.JavadocTestCase
import org.ratpackframework.manual.fixture.JavadocTests
import org.ratpackframework.manual.fixture.DefaultScriptRunner

class RatpackJavadocTests extends JavadocTestCase {

  @Override
  protected void addTests(JavadocTests tests) {
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
        tests.testCodeSnippets(mainSrc, "**/*.java", "tested", new DefaultScriptRunner())

        def groovyChainDslPrefix = """
          import org.ratpackframework.groovy.handling.Chain;
          import org.ratpackframework.groovy.handling.internal.DefaultChain;
          import org.ratpackframework.groovy.Util;

          Util.configureDelegateOnly((Chain) new DefaultChain([])) {
        """

        def groovyChainDslSuffix = """
          }
        """
        tests.testCodeSnippets(mainSrc, "**/*.java", "groovy-chain-dsl", new DefaultScriptRunner(groovyChainDslPrefix, groovyChainDslSuffix))
      }
    }
  }
}
