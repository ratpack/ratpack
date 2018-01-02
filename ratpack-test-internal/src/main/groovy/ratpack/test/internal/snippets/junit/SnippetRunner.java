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

package ratpack.test.internal.snippets.junit;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import ratpack.test.internal.snippets.TestCodeSnippet;

import static ratpack.util.Exceptions.uncheck;

public class SnippetRunner extends Runner {

  private final Description description;
  private final TestCodeSnippet snippet;

  public SnippetRunner(Class<?> testClass, TestCodeSnippet snippet) {
    this.description = Description.createTestDescription(testClass, snippet.getTestName());
    this.snippet = snippet;
  }

  @Override
  public Description getDescription() {
    return description;
  }

  @Override
  public void run(RunNotifier notifier) {
    Description description = getDescription();
    String filter = System.getProperty("filter");
    if (filter != null && !filter.equals(description.getMethodName())) {
      notifier.fireTestIgnored(description);
      return;
    }

    try {
      notifier.fireTestStarted(description);
      snippet.getExecuter().execute(snippet);
    } catch (Throwable t) {
      Throwable transform;
      try {
        transform = snippet.getExceptionTransformer().transform(t, snippet.getExecuter().getFixture().getOffset());
      } catch (Exception e) {
        throw uncheck(e);
      }
      notifier.fireTestFailure(new Failure(description, transform));
    } finally {
      notifier.fireTestFinished(description);
    }
  }

  @Override
  public int testCount() {
    return 1;
  }


}
