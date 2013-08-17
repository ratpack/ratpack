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

package org.ratpackframework.manual.snippets

import org.junit.runner.Runner
import org.ratpackframework.manual.snippets.junit.SnippetRunner

class DefaultCodeSnippetTests implements CodeSnippetTests {

  private final Class<?> clazz
  private final List<Runner> runners
  private final SnippetExecuter executer;

  DefaultCodeSnippetTests(Class<?> clazz, List<Runner> runners, SnippetExecuter executer) {
    this.clazz = clazz
    this.runners = runners
    this.executer = executer
  }

  public void add(TestCodeSnippet snippet) {
    runners.add(new SnippetRunner(clazz, executer, snippet));
  }

}
