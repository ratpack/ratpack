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

package org.ratpackframework.jackson;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import org.ratpackframework.jackson.internal.DefaultJsonRenderer;

import javax.inject.Singleton;

/**
 * A Guice module that provides an implementation of {@link JsonRenderer}, a renderer for {@link Json} object.
 * <p>
 * Also provides a default instance of {@link ObjectMapper}, which is the engine for serialization, and an
 * instance of {@link ObjectWriter} derived from this which is used by the {@link JsonRenderer} implementation.
 * To globally customize JSON generation, It is usually sufficient to override the {@link ObjectMapper} binding.
 */
@SuppressWarnings("UnusedDeclaration")
public class JacksonModule extends AbstractModule {

  boolean prettyPrint;

  /**
   * Constructor.
   */
  public JacksonModule() {
  }

  /**
   * Constructor.
   *
   * @param prettyPrint initial pretty print value.
   */
  public JacksonModule(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
  }

  /**
   * Should the JSON be pretty printed.
   * <p>
   * Default is {@code false}.
   *
   * @return Should the JSON be pretty printed.
   */
  public boolean isPrettyPrint() {
    return prettyPrint;
  }

  /**
   * Sets if the JSON should be pretty printed.
   * <p>
   * See {@link DefaultPrettyPrinter} for formatting details.
   *
   * @param prettyPrint If the JSON should be pretty printed
   */
  public void setPrettyPrint(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
  }

  @Override
  protected void configure() {
    bind(JsonRenderer.class).to(DefaultJsonRenderer.class);
    bind(ObjectMapper.class).in(Scopes.SINGLETON);
  }

  /**
   * Creates an object writer based on the bound {@link ObjectMapper} and the {@link #isPrettyPrint()} value.
   *
   * @param objectMapper The object mapper.
   * @return An object writer.
   */
  @Provides
  @Singleton
  ObjectWriter objectWriter(ObjectMapper objectMapper) {
    return objectMapper.writer(isPrettyPrint() ? new DefaultPrettyPrinter() : new MinimalPrettyPrinter());
  }

}
