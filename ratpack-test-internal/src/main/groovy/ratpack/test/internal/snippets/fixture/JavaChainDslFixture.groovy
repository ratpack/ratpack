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

package ratpack.test.internal.snippets.fixture

class JavaChainDslFixture extends SnippetFixture {

  @Override
  String pre() {
    """
import ratpack.func.Action;
import ratpack.server.ServerConfig;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.handling.Context;
import ratpack.handling.internal.DefaultChain;
import com.google.common.collect.Lists;
import java.util.List;

public class Example {
  public static void main(String... args) throws Exception {
    List<Handler> handlers = Lists.newLinkedList();
    ServerConfig serverConfig = ServerConfig.embedded().development(false).build();
    Chain chain = new DefaultChain(handlers, serverConfig, null);
"""
  }

  @Override
  String post() {
    """
  }
}
"""
  }
}
