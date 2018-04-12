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

import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import ratpack.test.internal.snippets.junit.DelegatingTestRunner;
import ratpack.test.internal.snippets.junit.RunnerProvider;

import java.util.LinkedList;
import java.util.List;

@RunWith(DelegatingTestRunner.class)
abstract public class CodeSnippetTestCase implements RunnerProvider {

  protected abstract void addTests(CodeSnippetTests tests);

  public final List<Runner> getRunners() {
    List<Runner> runners = new LinkedList<>();
    CodeSnippetTests tests = new DefaultCodeSnippetTests(getClass(), runners);
    addTests(tests);
    return runners;
  }

}
