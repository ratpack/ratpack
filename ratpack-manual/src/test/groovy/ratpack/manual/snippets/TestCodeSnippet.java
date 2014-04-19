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

package ratpack.manual.snippets;

import ratpack.manual.snippets.fixture.SnippetFixture;
import ratpack.func.Transformer;

public class TestCodeSnippet {

  private final String snippet;
  private final String className;
  private final String testName;
  private final SnippetFixture fixture;
  private final Transformer<Throwable, Throwable> exceptionTransformer;

  public TestCodeSnippet(String snippet, String className, String testName, SnippetFixture fixture, Transformer<Throwable, Throwable> exceptionTransformer) {
    this.snippet = snippet;
    this.className = className;
    this.testName = testName;
    this.fixture = fixture;
    this.exceptionTransformer = exceptionTransformer;
  }

  public String getSnippet() {
    return snippet;
  }

  public String getClassName() {
    return className;
  }

  public String getTestName() {
    return testName;
  }

  public SnippetFixture getFixture() {
    return fixture;
  }

  public String getCompleteSnippet() {
    return fixture.pre() + snippet + fixture.post();
  }

  public Transformer<Throwable, Throwable> getExceptionTransformer() {
    return exceptionTransformer;
  }

}
