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

package org.ratpackframework.manual.snippets.junit;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.ratpackframework.manual.snippets.SnippetExecuter;
import org.ratpackframework.manual.snippets.TestCodeSnippet;

public class SnippetRunner extends Runner {

  private final Description description;
  private final SnippetExecuter executer;
  private final TestCodeSnippet snippet;

  public SnippetRunner(Class<?> testClass, SnippetExecuter executer, TestCodeSnippet snippet) {
    this.description = Description.createTestDescription(testClass, snippet.getTestName());
    this.executer = executer;
    this.snippet = snippet;
  }

  @Override
  public Description getDescription() {
    return description;
  }

  @Override
  public void run(RunNotifier notifier) {
    try {
      notifier.fireTestStarted(getDescription());
      executer.execute(snippet);
    } catch (Throwable t) {
      notifier.fireTestFailure(new Failure(getDescription(), snippet.getExceptionTransformer().transform(t)));
    } finally {
      notifier.fireTestFinished(getDescription());
    }
  }

  @Override
  public int testCount() {
    return 1;
  }


}
