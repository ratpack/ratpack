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

package ratpack.test.internal.snippets;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static ratpack.util.Exceptions.uncheck;

abstract public class CodeSnippetTestCase {

  protected abstract Collection<TestCodeSnippet> registerTests();

  @TestFactory
  public Stream<DynamicTest> getTests() {
    return DynamicTest.stream(
      registerTests().stream(),
      TestCodeSnippet::getTestName,
      snippet -> {
        String filter = System.getProperty("filter");
        assumeTrue(filter == null || filter.equals(snippet.getTestName()));
        assertTimeout(Duration.ofSeconds(30), () -> {
          try {
            snippet.getExecuter().execute(snippet);
          } catch (Throwable t) {
            Throwable transform;
            try {
              transform = snippet.getExceptionTransformer().transform(t, snippet.getExecuter().getFixture().getOffset());
            } catch (Exception e) {
              throw uncheck(e);
            }
            throw transform;
          }
        });
      });
  }

}
