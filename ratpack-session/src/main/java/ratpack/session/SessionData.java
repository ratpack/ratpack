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

package ratpack.session;

import ratpack.exec.Operation;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * The data associated with the user session.
 * <p>
 * The session data can be obtained via the {@link Session#getData()} method.
 * <p>
 * Every item is stored with a {@link SessionKey}.
 * A key is comprised of a type (as a {@code Class}) and an optional name.
 * The combination of type and name identifies the value.
 * Two keys with differing types but identical names are not considered to be equivalent.
 * <p>
 * When writing session data, the values are serialized immediately.
 * That is, all objects are treated as immutable value objects from the perspective of this object.
 * Any changes made to an object after it has been written to this object will <b>NOT</b> be persisted.
 * If such changes are to be persisted, the object must be written again.
 * <p>
 * If a {@link SessionSerializer} is not provided for a given get/set, the {@link #getDefaultSerializer()} will be used.
 * The {@link SessionModule} provides a default implementation based on Java serialization.
 * An alternative implementation can be used by overriding the Guice binding for {@link SessionSerializer}.
 * <p>
 * The session data is held in memory for a given request until the {@link Session#save()} method is called on the corresponding session.
 * However, this object internally holds the data in serialized form.
 * Therefore, every read and write incurs the cost of serialization and deserialization.
 */
public interface SessionData {

  /**
   * The keys of all objects currently in the session.
   *
   * @return the keys of all objects currently in the session
   */
  Set<SessionKey<?>> getKeys();

  /**
   * Fetch the object with the given key, using the {@link #getDefaultSerializer() default serializer}.
   *
   * @param key the key
   * @param <T> the type of object
   * @return the value for the given key
   * @see #require(SessionKey)
   */
  default <T> Optional<T> get(SessionKey<T> key) throws Exception {
    return get(key, getDefaultSerializer());
  }

  /**
   * Read the object with the given key.
   *
   * @param key the key
   * @param serializer the serializer
   * @param <T> the type of object
   * @return the value for the given key
   * @see #require(SessionKey, SessionSerializer)
   */
  <T> Optional<T> get(SessionKey<T> key, SessionSerializer serializer) throws Exception;

  /**
   * Read the object with the given name, using the {@link #getDefaultSerializer() default serializer}.
   * <p>
   * This method will throw an {@link IllegalArgumentException} if there is more than one object who's key has the given name.
   *
   * @param name the object name
   * @return the value for the given key
   * @see #require(String)
   */
  default Optional<?> get(String name) throws Exception {
    return get(SessionKey.of(name));
  }

  /**
   * Read the object with the given name.
   * <p>
   * This method will throw an {@link IllegalArgumentException} if there is more than one object who's key has the given name.
   *
   * @param name the object name
   * @param serializer the serializer
   * @return the value for the given key
   * @see #require(String, SessionSerializer)
   */
  default Optional<?> get(String name, SessionSerializer serializer) throws Exception {
    return get(SessionKey.of(name), serializer);
  }

  /**
   * Read the object with the given type, and no name, using the {@link #getDefaultSerializer() default serializer}.
   *
   * @param type the type
   * @param <T> the type
   * @return the value for the given key
   * @see #require(Class)
   */
  default <T> Optional<T> get(Class<T> type) throws Exception {
    return get(SessionKey.of(type));
  }

  /**
   * Read the object with the given type, and no name.
   *
   * @param type the type
   * @param serializer the serializer
   * @param <T> the type
   * @return the value for the given key
   * @see #require(Class, SessionSerializer)
   */
  default <T> Optional<T> get(Class<T> type, SessionSerializer serializer) throws Exception {
    return get(SessionKey.of(type), serializer);
  }

  /**
   * Like {@link #get(SessionKey)}, but throws {@link NoSuchElementException} on the absence of a value.
   *
   * @param key the object key
   * @param <T> the type
   * @return the value for the given key
   */
  default <T> T require(SessionKey<T> key) throws Exception {
    return require(key, getDefaultSerializer());
  }

  /**
   * Like {@link #get(SessionKey, SessionSerializer)}, but throws {@link NoSuchElementException} on the absence of a value.
   *
   * @param key the object key
   * @param serializer the serializer
   * @param <T> the type
   * @return the value for the given key
   */
  default <T> T require(SessionKey<T> key, SessionSerializer serializer) throws Exception {
    return get(key, serializer).orElseThrow(() -> new NoSuchElementException("No object for " + key + " in session"));
  }

  /**
   * Like {@link #get(Class)}, but throws {@link NoSuchElementException} on the absence of a value.
   *
   * @param type the type
   * @param <T> the type
   * @return the value for the given key
   */
  default <T> T require(Class<T> type) throws Exception {
    return require(SessionKey.of(type));
  }

  /**
   * Like {@link #get(Class, SessionSerializer)}, but throws {@link NoSuchElementException} on the absence of a value.
   *
   * @param type the type
   * @param serializer the serializer
   * @param <T> the type
   * @return the value for the given key
   */
  default <T> T require(Class<T> type, SessionSerializer serializer) throws Exception {
    return require(SessionKey.of(type), serializer);
  }

  /**
   * Like {@link #get(String)}, but throws {@link NoSuchElementException} on the absence of a value.
   * <p>
   * This method will throw an {@link IllegalArgumentException} if there is more than one object who's key has the given name.
   *
   * @param name the object name
   * @return the value for the given key
   */
  default Object require(String name) throws Exception {
    return require(SessionKey.of(name));
  }

  /**
   * Like {@link #get(String, SessionSerializer)}, but throws {@link NoSuchElementException} on the absence of a value.
   * <p>
   * This method will throw an {@link IllegalArgumentException} if there is more than one object who's key has the given name.
   *
   * @param name the object name
   * @param serializer the serializer
   * @return the value for the given key
   */
  default Object require(String name, SessionSerializer serializer) throws Exception {
    return require(SessionKey.of(name), serializer);
  }

  /**
   * Sets the value for the given key, using the {@link #getDefaultSerializer() default serializer}.
   *
   * @param key the key
   * @param value the value
   * @param <T> the type
   */
  default <T> void set(SessionKey<T> key, T value) throws Exception {
    set(key, value, getDefaultSerializer());
  }

  /**
   * Sets the value for the given key.
   *
   * @param key the key
   * @param value the value
   * @param serializer the serializer
   * @param <T> the type
   */
  <T> void set(SessionKey<T> key, T value, SessionSerializer serializer) throws Exception;

  /**
   * Sets the value for the given type, using the {@link #getDefaultSerializer() default serializer}.
   *
   * @param type the type
   * @param value the value
   * @param <T> the type
   */
  default <T> void set(Class<T> type, T value) throws Exception {
    set(SessionKey.of(type), value);
  }

  /**
   * Sets the value for the given type.
   *
   * @param type the type
   * @param value the value
   * @param serializer the serializer
   * @param <T> the type
   */
  default <T> void set(Class<T> type, T value, SessionSerializer serializer) throws Exception {
    set(SessionKey.of(type), value, serializer);
  }

  /**
   * Sets the value for the given name and type, using the runtime type of the value and the {@link #getDefaultSerializer() default serializer}.
   *
   * @param name the name
   * @param value the value
   * @param <T> the type
   */
  default <T> void set(String name, T value) throws Exception {
    set(SessionKey.ofType(name, value), value, getDefaultSerializer());
  }

  /**
   * Sets the value for the given name and type, using the runtime type of the value.
   *
   * @param name the name
   * @param value the value
   * @param serializer the serializer
   * @param <T> the type
   */
  default <T> void set(String name, T value, SessionSerializer serializer) throws Exception {
    set(SessionKey.ofType(name, value), value, serializer);
  }

  /**
   * Sets the value for the type, using the runtime type of the value and the {@link #getDefaultSerializer() default serializer}.
   *
   * @param value the value
   * @param <T> the type
   */
  default <T> void set(T value) throws Exception {
    set(SessionKey.ofType(value), value, getDefaultSerializer());
  }

  /**
   * Sets the value for the type, using the runtime type of the value.
   *
   * @param value the value
   * @param serializer the serializer
   * @param <T> the type
   */
  default <T> void set(T value, SessionSerializer serializer) throws Exception {
    set(SessionKey.ofType(value), value, serializer);
  }

  /**
   * Removes the object with the given key, if it exists.
   *
   * @param key the key
   */
  void remove(SessionKey<?> key);

  /**
   * Removes the object with the given type and no name, if it exists.
   *
   * @param type the type
   */
  default void remove(Class<?> type) {
    remove(SessionKey.of(type));
  }

  /**
   * Removes the object with the name, if it exists.
   * <p>
   * This method will throw a {@link NoSuchElementException} if there are more than one entries with the given name.
   *
   * @param name the name
   */
  default void remove(String name) {
    remove(SessionKey.of(name));
  }

  /**
   * Remove all entries from the session data.
   */
  void clear();

  /**
   * The corresponding {@link Session} object.
   *
   * @return the session object
   */
  Session getSession();

  /**
   * See {@link Session#isDirty()}.
   *
   * @return whether or not any changes have been made to the session data since it was accessed
   */
  default boolean isDirty() {
    return getSession().isDirty();
  }

  /**
   * See {@link Session#save()}.
   *
   * @return the save operation
   */
  default Operation save() {
    return getSession().save();
  }

  /**
   * See {@link Session#terminate()}.
   *
   * @return the terminate operation
   */
  default Operation terminate() {
    return getSession().terminate();
  }

  /**
   * See {@link Session#getDefaultSerializer()}.
   *
   * @return the serializer to use by default
   */
  default SessionSerializer getDefaultSerializer() {
    return getSession().getDefaultSerializer();
  }

  /**
   * See {@link Session#getJavaSerializer()}.
   *
   * @return a serializer for {@link Serializable} objects
   */
  default JavaSessionSerializer getJavaSerializer() {
    return getSession().getJavaSerializer();
  }

}
