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

package ratpack.jackson.configuration.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.io.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.configuration.Configuration;
import ratpack.configuration.ConfigurationException;
import ratpack.configuration.ConfigurationFactory;
import ratpack.configuration.ConfigurationSource;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class JacksonConfigurationFactory implements ConfigurationFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(JacksonConfigurationFactory.class);

  private final YAMLFactory yamlFactory = new YAMLFactory();
  private final Validator validator = buildValidator();
  private final ObjectMapper objectMapper = buildObjectMapper();

  @Override
  public <T extends Configuration> T build(Class<T> configurationClass, ConfigurationSource configurationSource) throws ConfigurationException {
    ObjectNode node;
    ByteSource byteSource = configurationSource.getByteSource();
    try {
      if (byteSource.isEmpty()) {
        node = JsonNodeFactory.instance.objectNode();
      } else {
        try (InputStream inputStream = configurationSource.getByteSource().openBufferedStream()) {
          YAMLParser yamlParser = yamlFactory.createParser(inputStream);
          node = objectMapper.readTree(yamlParser);
        }
      }
      return build(configurationClass, node);
    } catch (IOException ex) {
      throw new ConfigurationException("Failed to load configuration", ex);
    }
  }

  private <T extends Configuration> T build(Class<T> configurationClass, ObjectNode node) throws IOException, ConfigurationException {
    // TODO: property overrides
    // TODO: environment overrides
    T configuration = objectMapper.readValue(new TreeTraversingParser(node), configurationClass);
    if (validator != null) {
      Set<ConstraintViolation<T>> violations = validator.validate(configuration);
      if (!violations.isEmpty()) {
        throw new ConfigurationException("Configuration failed validation: " + violations.toString());
      }
    }
    return configuration;
  }

  private static Validator buildValidator() {
    try {
      return Validation.buildDefaultValidatorFactory().getValidator();
    } catch (ValidationException ex) {
      LOGGER.warn("Could not load a validation provider");
      LOGGER.debug("", ex);
      return null;
    }
  }

  private static ObjectMapper buildObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new GuavaModule());
    objectMapper.setSubtypeResolver(new DiscoverableSubtypeResolver());
    return objectMapper;
  }
}
