/*
 * Copyright 2021 the original author or authors.
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

package ratpack.session;

import com.google.inject.AbstractModule;
import ratpack.guice.Guice;
import ratpack.test.embed.EmbeddedApp;

import java.io.Serializable;

import static org.junit.Assert.assertEquals;

public class Example {

  static class AllowedType implements Serializable {
  }

  static class NonAllowedType implements Serializable {
  }

  static class NonAllowedTypeContainer implements Serializable {
    NonAllowedType nonAllowedType = new NonAllowedType();
  }

  public static class MySessionTypesModule extends AbstractModule {
    @Override
    protected void configure() {
      SessionModule.allowTypes(binder(),
        AllowedType.class,
        NonAllowedTypeContainer.class
      );
    }
  }

  public static void main(String... args) throws Exception {
    EmbeddedApp.of(a -> a
      .registry(Guice.registry(b -> b
        .module(SessionModule.class)
        .module(MySessionTypesModule.class)
      ))
      .handlers(c -> c
        .get("set/allowed", ctx ->
          ctx.get(Session.class)
            .set(new AllowedType())
            .then(() -> ctx.render("ok"))
        )
        .get("set/nonAllowed", ctx ->
          ctx.get(Session.class)
            .set(new NonAllowedType())
            .onError(e -> ctx.render(e.toString()))
            .then(() -> ctx.render("ok"))
        )
        .get("set/nonAllowedContainer", ctx ->
          ctx.get(Session.class)
            .set(new NonAllowedTypeContainer())
            .onError(e -> ctx.render(e.toString()))
            .then(() -> ctx.render("ok"))
        )
      )
    ).test(http -> {
      // Works, only references allowed types
      String response1 = http.getText("set/allowed");
      assertEquals("ok", response1);

      // Doesn't work, tries to set not allowed type
      String response2 = http.getText("set/nonAllowed");
      assertEquals("ratpack.session.NonAllowedSessionTypeException: Type ratpack.session.Example$NonAllowedType is not an allowed session type. Register a SessionTypeFilterPlugin allowing it. See SessionModule.allowTypes().", response2);

      // Doesn't work, allowed type references not allowed type
      String response3 = http.getText("set/nonAllowedContainer");
      assertEquals("ratpack.session.NonAllowedSessionTypeException: Type ratpack.session.Example$NonAllowedType is not an allowed session type. Register a SessionTypeFilterPlugin allowing it. See SessionModule.allowTypes().", response2);
    });
  }
}
