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

package ratpack.util.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Properties;

import static java.lang.String.format;

/**
 * A delegate for {@link java.util.Properties} that can coerce values to various types.
 */
public class TypeCoercingProperties {

  private final Properties delegate;
  private final ClassLoader classLoader;

  public TypeCoercingProperties(Properties delegate) {
    this(delegate, TypeCoercingProperties.class.getClassLoader());
  }

  public TypeCoercingProperties(Properties delegate, ClassLoader classLoader) {
    this.delegate = delegate;
    this.classLoader = classLoader;
  }

  /**
   * Gets a property value as a String, substituting a default if the property does not exist.
   *
   * @param key the property key.
   * @param defaultValue the value to use if the property does not exist.
   * @return the property value as a String or <code>defaultValue</code> if it does not exist.
   * @see Properties#getProperty(String, String)
   */
  public String asString(String key, String defaultValue) {
    return delegate.getProperty(key, defaultValue);
  }

  /**
   * Gets a property value as a boolean, substituting a default if the property does not exist.
   *
   * @param key the property key.
   * @param defaultValue the value to use if the property does not exist.
   * @return the property value coerced to a boolean as specified by {@link Boolean#parseBoolean(String)} or <code>defaultValue</code> if it does not exist.
   */
  public boolean asBoolean(String key, boolean defaultValue) {
    return Boolean.parseBoolean(delegate.getProperty(key, Boolean.toString(defaultValue)));
  }

  /**
   * Gets a property value as an int, substituting a default if the property does not exist.
   *
   * @param key the property key.
   * @param defaultValue the value to use if the property does not exist.
   * @return the property value coerced to an int as specified by {@link Integer#parseInt(String)} or <code>defaultValue</code> if it does not exist.
   */
  public int asInt(String key, int defaultValue) {
    return Integer.parseInt(delegate.getProperty(key, Integer.toString(defaultValue)));
  }

  /**
   * Gets a property value as an long, substituting a default if the property does not exist.
   *
   * @param key the property key.
   * @param defaultValue the value to use if the property does not exist.
   * @return the property value coerced to an long as specified by {@link Long#parseLong(String)} or <code>defaultValue</code> if it does not exist.
   */
  public long asLong(String key, long defaultValue) {
    return Long.parseLong(delegate.getProperty(key, Long.toString(defaultValue)));
  }

  /**
   * Gets a property value as a URI.
   *
   * @param key the property key.
   * @return the URI represented by the property value or <code>null</code> if the property does not exist.
   * @throws URISyntaxException if the property value is not a valid URI.
   */
  public URI asURI(String key) throws URISyntaxException {
    URI uri = null;
    String uriString = delegate.getProperty(key);

    if (uriString != null) {
      uri = new URI(uriString);
    }
    return uri;
  }

  /**
   * Gets a property value as an InetAddress.
   *
   * @param key the property key.
   * @return the InetAddress represented by the property value or <code>null</code> if the property does not exist.
   * @throws UnknownHostException if the property value is not a valid address.
   */
  public InetAddress asInetAddress(String key) throws UnknownHostException {
    String addressString = delegate.getProperty(key);
    if (addressString == null) {
      return null;
    } else {
      return InetAddress.getByName(addressString);
    }
  }

  /**
   * Gets a property value as a List of Strings. Each element of the returned List is trimmed of leading and trailing whitespace. Empty elements are omitted from the List.
   *
   * @param key the property key.
   * @return a List of Strings created by treating the property value as a comma-separated list or an empty List if the property does not exist.
   */
  public List<String> asList(String key) {
    String delimitedValues = delegate.getProperty(key, "");
    ImmutableList.Builder<String> trimmed = ImmutableList.builder();
    for (String value : delimitedValues.split(",")) {
      value = value.trim();
      if (!value.isEmpty()) {
        trimmed.add(value);
      }
    }
    return trimmed.build();
  }

  /**
   * Gets a property value as a ByteSource. The property value can be any of:
   * <ul>
   *   <li>An absolute file path to a file that exists.</li>
   *   <li>A valid URI.</li>
   *   <li>A classpath resource path loaded via the ClassLoader passed to the constructor.</li>
   * </ul>
   *
   * @param  key the property key.
   * @return a ByteSource or {@code null} if the property does not exist.
   * @throws java.lang.IllegalArgumentException if the property value cannot be resolved to a byte source.
   */
  public ByteSource asByteSource(String key) {
    ByteSource byteSource = null;
    String path = delegate.getProperty(key);
    if (path != null) {
      // try to treat it as a File path
      File file = new File(path);
      if (file.isFile()) {
        byteSource = Files.asByteSource(file);
      } else {
        // try to treat it as a URL
        try {
          URL url = new URL(path);
          byteSource = Resources.asByteSource(url);
        } catch (MalformedURLException e) {
          // try to treat it as a resource path
          byteSource = Resources.asByteSource(Resources.getResource(path));
        }
      }
    }
    return byteSource;
  }

  /**
   * Gets a property value as an InputStream. The property value can be any of:
   * <ul>
   *   <li>An absolute file path to a file that exists.</li>
   *   <li>A valid URI.</li>
   *   <li>A classpath resource path loaded via the ClassLoader passed to the constructor.</li>
   * </ul>
   *
   * @param key the property key.
   * @return an InputStream or <code>null</code> if the property does not exist.
   * @throws FileNotFoundException if the property value cannot be resolved to a stream source.
   */
  public InputStream asStream(String key) throws IOException {
    InputStream stream = null;
    String path = delegate.getProperty(key);
    if (path != null) {
      // try to treat it as a File path
      File file = new File(path);
      if (file.isFile()) {
        stream = new FileInputStream(file);
      } else {
        // try to treat it as a URL
        try {
          URL url = new URL(path);
          stream = url.openStream();
        } catch (MalformedURLException e) {
          // try to treat it as a resource path
          stream = classLoader.getResourceAsStream(path);
          if (stream == null) {
            throw new FileNotFoundException(path);
          }
        }
      }
    }
    return stream;
  }

  /**
   * Gets a property value as a Class. The property value should be a fully-qualified class name that can be loaded via the ClassLoader passed to the constructor. The class should be an instance of the <code>type</code> parameter – typically an interface.
   *
   * @param key the property key.
   * @param type the expected type of the Class, typically the interface it should implement.
   * @return a Class or <code>null</code> if the property does not exist.
   * @throws ClassNotFoundException if no Class is found matching the name from the property value.
   * @throws ClassCastException if a Class is loaded but it does not implement the <code>type</code> interface.
   */
  @SuppressWarnings("unchecked")
  public <T> Class<T> asClass(String key, Class<T> type) throws ClassNotFoundException {
    String className = delegate.getProperty(key);
    if (className == null) {
      return null;
    }

    Class<?> untypedClass = classLoader.loadClass(className);
    if (!type.isAssignableFrom(untypedClass)) {
      throw new ClassCastException(format("Class '%s' does not implement '%s", className, type.getName()));
    }

    return (Class<T>) classLoader.loadClass(className);
  }

}
