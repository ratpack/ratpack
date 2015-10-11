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

package ratpack.groovy;

import ratpack.server.RatpackServer;

/**
 * A boilerplate entry point, which combines {@link RatpackServer#start(ratpack.func.Action)} and {@link Groovy.Script#app()}.
 * <p>
 * This can be be used as the main class, when defining your Ratpack app as a Groovy script.
 * The application arguments will be available as {@code this.args} (as a {@code String[]}) in the script.
 *
 * @see Groovy.Script#appWithArgs(String...)
 */
public class GroovyRatpackMain {

  /**
   * Starts the server defined by calling {@link Groovy.Script#appWithArgs(String...)}.
   *
   * @param args not used
   * @throws Exception any when starting the server
   */
  public static void main(String... args) throws Exception {
    RatpackServer.start(Groovy.Script.appWithArgs(args));
  }
}
