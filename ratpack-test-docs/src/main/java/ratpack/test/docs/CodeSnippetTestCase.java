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

package ratpack.test.docs;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.File;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static ratpack.func.Exceptions.uncheck;

/**
 * A base class for generating tests from documentation samples.
 * <p>
 * Utilizes JUnit 5's {@link DynamicTest} and {@link TestFactory} to generate tests at runtime.
 */
public abstract class CodeSnippetTestCase {

  public abstract SnippetExtractor getExtractor();

  public abstract String getIncludePath();

  /**
   * Specifies the root directory on the filesystem containing the Java source files.
   *
   * @return the source root directory.
   */
  public abstract File getSourceDirectory();

  /**
   * Specifies a mapping of CSS classes contained in the javadoc snippet to executor.
   * <p>
   * This allows for optimizing code samples by supplying static fixtures around their execution
   * without having to specify the full contents in the sample.
   * <p>
   * For example, different classes can specify the test entry point and import statements.
   * @return
   */
  public abstract Map<String, SnippetExecuter> getCssExecutorMapping();

  protected Collection<TestCodeSnippet> registerTests() {
    final File mainSrc = getSourceDirectory();
    if (mainSrc.exists()) {
      return getCssExecutorMapping().entrySet().stream()
        .map(e ->
          getExtractor().extract(mainSrc, getIncludePath(), e.getKey(), e.getValue())
        )
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    } else {
      return Collections.emptyList();
    }
  }

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
