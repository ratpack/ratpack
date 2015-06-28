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

package ratpack.jackson.guice;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.Lists;
import com.google.inject.Provides;
import ratpack.guice.ConfigurableModule;
import ratpack.jackson.Jackson;
import ratpack.jackson.JsonParseOpts;
import ratpack.jackson.JsonRender;
import ratpack.parse.NullParseOpts;
import ratpack.parse.Parser;
import ratpack.render.Renderer;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Integrates the <a href="https://github.com/FasterXML/jackson">Jackson JSON processing library</a>.
 * <p>
 * See the {@link Jackson} class documentation for usage examples.
 * <h3>Provided Objects</h3>
 * <p>
 * Provides singleton instances of the following Jackson types:
 * <ul>
 * <li>{@link com.fasterxml.jackson.databind.ObjectMapper}</li>
 * <li>{@link com.fasterxml.jackson.databind.ObjectWriter}</li>
 * <li>{@link com.fasterxml.jackson.databind.ObjectReader}</li>
 * </ul>
 * <p>
 * Also Provides singleton instances of the following Ratpack specific types:
 * <ul>
 * <li><code>{@link Renderer}&lt;{@link JsonRender}&gt;</code> (with content type {@code "application/json"})</li>
 * <li><code>{@link Parser}&lt;{@link JsonParseOpts}&gt;</code> (with content type {@code "application/json"})</li>
 * <li><code>{@link Parser}&lt;{@link NullParseOpts}&gt;</code> (with content type {@code "application/json"})</li>
 * </ul>
 * <p>
 * The above support the renderable/parseable types provided by the {@link Jackson} class.
 * <h3>Configuration</h3>
 * <p>
 * This module follows Ratpack's {@link ConfigurableModule configurable module} pattern, using the {@link Config} type as the configuration.
 * <p>Note that Jackson feature modules can be conveniently registered via the {@link JacksonModule.Config#modules(Iterable)} method.
 * <pre class="java">{@code
 * import ratpack.guice.Guice;
 * import ratpack.test.embed.EmbeddedApp;
 * import ratpack.jackson.JacksonModule;
 * import ratpack.http.client.ReceivedResponse;
 * import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
 *
 * import java.util.Optional;
 *
 * import static ratpack.jackson.Jackson.json;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *
 *   public static class Person {
 *     private final String name;
 *     public Person(String name) {
 *       this.name = name;
 *     }
 *     public String getName() {
 *       return name;
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *       .registry(Guice.registry(b ->
 *         b.module(JacksonModule.class, c -> c
 *           .modules(new Jdk8Module()) // register the Jackson module
 *           .prettyPrint(false)
 *         )
 *       ))
 *       .handlers(chain ->
 *         chain.get(ctx -> {
 *           Optional<Person> personOptional = Optional.of(new Person("John"));
 *           ctx.render(json(personOptional));
 *         })
 *       )
 *     ).test(httpClient -> {
 *       ReceivedResponse response = httpClient.get();
 *       assertEquals("{\"name\":\"John\"}", response.getBody().getText());
 *       assertEquals("application/json", response.getBody().getContentType().getType());
 *     });
 *   }
 * }
 * }</pre>
 * <p>
 * In the above example, the Jackson capabilities are being extended by use of the <a href="https://github.com/FasterXML/jackson-datatype-jdk8">additional JDK 8 datatype module</a>.
 *
 * @see Jackson
 */
@SuppressWarnings("UnusedDeclaration")
public class JacksonModule extends ConfigurableModule<JacksonModule.Config> {

  /**
   * The configuration object for {@link JacksonModule}.
   */
  public static class Config {
    private boolean prettyPrint = true;
    private List<Module> modules = Lists.newLinkedList();
    private List<Consumer<? super ObjectMapper>> configurers = Lists.newLinkedList();

    /**
     * Whether JSON should be pretty printed.
     * <p>
     * {@code true} by default.
     *
     * @return whether JSON should be pretty printed.
     */
    public boolean isPrettyPrint() {
      return prettyPrint;
    }

    /**
     * Sets whether JSON should be pretty printed.
     * <p>
     * This value affects the {@link ObjectWriter} instance provided by this Guice module.
     * If this binding is overridden, calling this method may have no effect.
     *
     * @param prettyPrint whether JSON should be pretty printed
     * @return this
     */
    public Config prettyPrint(boolean prettyPrint) {
      this.prettyPrint = prettyPrint;
      return this;
    }

    /**
     * The {@link com.fasterxml.jackson.databind.Module Jackson modules} to register with the object mapper.
     * <p>
     * Jackson modules extend Jackson to handle extra data types.
     *
     * @return the Jackson modules to register with the object mapper
     * @see #modules(Iterable)
     * @see #modules(Module...)
     */
    public List<Module> getModules() {
      return modules;
    }

    /**
     * Add the given modules to the modules to be registered with the created object mapper.
     * <p>
     * This affects the {@link ObjectMapper} instance provided by this Guice module.
     * If this binding is overridden, calling this method may have no effect.
     *
     * @param modules the Jackson modules
     * @return this
     * @see #modules(Module...)
     */
    public Config modules(Iterable<Module> modules) {
      this.modules.addAll(Lists.newArrayList(modules));
      return this;
    }

    /**
     * Add the given modules to the modules to be registered with the created object mapper.
     * <p>
     * This affects the {@link ObjectMapper} instance provided by this Guice module.
     * If this binding is overridden, calling this method may have no effect.
     *
     * @param modules the Jackson modules
     * @return this
     * @see #modules(Iterable)
     */
    public Config modules(Module... modules) {
      return modules(Arrays.asList(modules));
    }

    /**
     * Adds configuration actions to be executed against the object mapper created by this Guice module before it is used.
     * <p>
     * This method is cumulative, with configure actions being executed in order.
     * They are executed <b>after</b> applying other relevant configuration to the mapper (e.g. registering modules).
     * <p>
     * This affects the {@link ObjectMapper} instance provided by this Guice module.
     * If this binding is overridden, calling this method may have no effect.
     *
     * @param configurer configuration for the object mapper
     * @return this
     */
    public Config withMapper(Consumer<? super ObjectMapper> configurer) {
      this.configurers.add(configurer);
      return this;
    }
  }

  @Override
  protected void configure() {
  }

  @Provides
  @Singleton
  protected ObjectMapper objectMapper(Config config) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModules(config.getModules());
    for (Consumer<? super ObjectMapper> configurer : config.configurers) {
      configurer.accept(objectMapper);
    }
    return objectMapper;
  }

  @Provides
  @Singleton
  protected ObjectWriter objectWriter(ObjectMapper objectMapper, Config config) {
    return objectMapper.writer(config.isPrettyPrint() ? new DefaultPrettyPrinter() : new MinimalPrettyPrinter());
  }

  @Provides
  @Singleton
  protected ObjectReader objectReader(ObjectMapper objectMapper) {
    return objectMapper.reader();
  }

  @Provides
  @Singleton
  protected Renderer<JsonRender> renderer(ObjectWriter objectWriter) {
    return Jackson.Init.renderer(objectWriter);
  }

  @Provides
  @Singleton
  protected Parser<NullParseOpts> noOptParser() {
    return Jackson.Init.noOptParser();
  }

  @Provides
  @Singleton
  protected Parser<JsonParseOpts> parser(ObjectMapper objectMapper) {
    return Jackson.Init.parser(objectMapper);
  }

}
