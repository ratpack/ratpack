/*
 * Copyright 2015 the original author or authors.
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

package ratpack.test.internal.snippets.fixture;

import ratpack.test.internal.snippets.executer.SnippetExecuter;
import ratpack.server.RatpackServer;
import ratpack.test.ServerBackedApplicationUnderTest;
import ratpack.test.http.TestHttpClient;

import static org.junit.Assert.assertEquals;

public class HelloWorldAppSnippetExecuter extends ServerCaptureSnippetExecuter {

  public HelloWorldAppSnippetExecuter(SnippetExecuter executer) {
    super(executer);
  }

  @Override
  protected void withServer(RatpackServer server) throws Exception {
    try {
      TestHttpClient httpClient = TestHttpClient.testHttpClient(ServerBackedApplicationUnderTest.of(server));
      assertEquals("Hello World!", httpClient.getText());
      assertEquals("Hello Thing!", httpClient.getText("Thing"));
    } finally {
      server.stop();
    }
  }
}
