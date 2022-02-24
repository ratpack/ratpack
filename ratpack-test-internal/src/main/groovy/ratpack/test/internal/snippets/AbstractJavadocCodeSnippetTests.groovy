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
import ratpack.test.docs.CodeSnippetTestCase
import ratpack.test.docs.SnippetExecuter
import ratpack.test.docs.SnippetExtractor
import ratpack.test.docs.SnippetFixture
import ratpack.test.docs.executer.GroovySnippetExecuter
import ratpack.test.docs.executer.JavaSnippetExecuter
import ratpack.test.docs.extractor.JavadocSnippetExtractor
import ratpack.test.internal.snippets.fixture.GroovyChainDslFixture
import ratpack.test.internal.snippets.fixture.GroovyRatpackDslNoRunFixture
import ratpack.test.docs.fixture.GroovyScriptFixture
import ratpack.test.internal.snippets.fixture.JavaChainDslFixture

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
  SnippetExtractor getExtractor() {
    return new JavadocSnippetExtractor()
  }

  @Override
  String getIncludePath() {
    return "**/*.java"
  }

  @Override
  File getSourceDirectory() {
    File cwd = new File(StandardSystemProperty.USER_DIR.value())
    File root
    if (new File(cwd, "${projectName}.gradle").exists()) {
      root = cwd
    } else {
      root = new File(cwd, projectName)
    }

    return  new File(root, "src/main")
  }

  @Override
  Map<String, SnippetExecuter> getCssExecutorMapping() {
    return FIXTURES
  }
}
