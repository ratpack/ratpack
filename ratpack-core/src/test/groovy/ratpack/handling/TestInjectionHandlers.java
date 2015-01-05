/*
 * Copyright 2014 the original author or authors.
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

package ratpack.handling;

import ratpack.server.ServerConfig;

@SuppressWarnings({"UnusedParameters", "UnusedDeclaration"})
public class TestInjectionHandlers {

  public static class MethodIsProtected extends InjectionHandler {
    protected void handle(Context context, ServerConfig serverConfig) {
      context.render("ok");
    }
  }

  public static class MethodIsPrivate extends InjectionHandler {
    private void handle(Context context, ServerConfig serverConfigg) {
      context.render("ok");
    }
  }

  public class PublicInnerWithPrivate extends InjectionHandler {
    private void handle(Context context, ServerConfig serverConfig) {
      context.render("ok");
    }
  }

  private class PrivateInnerWithPrivate extends InjectionHandler {
    private void handle(Context context, ServerConfig serverConfig) {
      context.render("ok");
    }
  }

  public Handler publicInnerWithPrivate() {
    return new PublicInnerWithPrivate();
  }

  public Handler privateInnerWithPrivate() {
    return new PrivateInnerWithPrivate();
  }


}
