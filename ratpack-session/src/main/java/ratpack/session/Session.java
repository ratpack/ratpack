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

import io.netty.util.AsciiString;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.http.Response;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

/**
 * A mechanism for associating semi persistent data with an individual user/client.
 * <p>
 * This object can be accessed via the context registry.
 * Access to the actual session data is provided by {@link #getData()} method this object.
 * <p>
 * The persistence mechanism used is determined by the implementation of {@link SessionStore} available.
 *
 * @see SessionModule
 */
public interface Session {

  /**
   * The unique ID for this session.
   * <p>
   * Call this method will provision a new ID if necessary.
   * Provisioning and tracking of the ID is provided by the bound {@link SessionId} implementation.
   *
   * @return the ID for this session
   */
  String getId();

  /**
   * The session data.
   * <p>
   * The data is available via a promise to support backing {@link SessionStore} implementations that load the data asynchronously.
   *
   * @return the session data
   */
  Promise<SessionData> getData();

  /**
   * A convenience shorthand for {@link SessionData#getKeys()}.
   *
   * @return the keys of all objects currently in the session
   */
  default Promise<Set<SessionKey<?>>> getKeys() {
    return getData().map(SessionData::getKeys);
  }

  /**
   * A convenience shorthand for {@link SessionData#get(SessionKey)}.
   *
   * @param key the key
   * @param <T> the type of object
   * @return the value for the given key
   * @see #require(SessionKey)
   */
  default <T> Promise<Optional<T>> get(SessionKey<T> key) {
    return getData().map(d -> d.get(key));
  }

  /**
   * A convenience shorthand for {@link SessionData#get(SessionKey, SessionSerializer)}.
   *
   * @param key the key
   * @param serializer the serializer
   * @param <T> the type of object
   * @return the value for the given key
   * @see #require(SessionKey, SessionSerializer)
   */
  default <T> Promise<Optional<T>> get(SessionKey<T> key, SessionSerializer serializer) {
    return getData().map(d -> d.get(key, serializer));
  }

  /**
   * A convenience shorthand for {@link SessionData#get(String)}.
   *
   * @param name the object name
   * @return the value for the given key
   * @see #require(String)
   */
  default Promise<Optional<?>> get(String name) {
    return getData().map(d -> d.get(name));
  }

  /**
   * A convenience shorthand for {@link SessionData#get(String, SessionSerializer)}.
   *
   * @param name the object name
   * @param serializer the serializer
   * @return the value for the given key
   * @see #require(String, SessionSerializer)
   */
  default Promise<Optional<?>> get(String name, SessionSerializer serializer) {
    return getData().map(d -> d.get(name, serializer));
  }

  /**
   * A convenience shorthand for {@link SessionData#get(Class)}.
   *
   * @param type the type
   * @param <T> the type
   * @return the value for the given key
   * @see #require(Class)
   */
  default <T> Promise<Optional<T>> get(Class<T> type) {
    return getData().map(d -> d.get(type));
  }

  /**
   * A convenience shorthand for {@link SessionData#get(Class, SessionSerializer)}.
   *
   * @param type the type
   * @param serializer the serializer
   * @param <T> the type
   * @return the value for the given key
   * @see #require(Class, SessionSerializer)
   */
  default <T> Promise<Optional<T>> get(Class<T> type, SessionSerializer serializer) {
    return getData().map(d -> d.get(type, serializer));
  }

  /**
   * A convenience shorthand for {@link SessionData#require(SessionKey)}.
   *
   * @param key the object key
   * @param <T> the type
   * @return the value for the given key
   */
  default <T> Promise<T> require(SessionKey<T> key) {
    return getData().map(d -> d.require(key));
  }

  /**
   * A convenience shorthand for {@link SessionData#require(SessionKey, SessionSerializer)}.
   *
   * @param key the object key
   * @param serializer the serializer
   * @param <T> the type
   * @return the value for the given key
   */
  default <T> Promise<T> require(SessionKey<T> key, SessionSerializer serializer) {
    return getData().map(d -> d.require(key, serializer));
  }

  /**
   * A convenience shorthand for {@link SessionData#require(Class)}.
   *
   * @param type the type
   * @param <T> the type
   * @return the value for the given key
   */
  default <T> Promise<T> require(Class<T> type) {
    return getData().map(d -> d.require(type));
  }

  /**
   * A convenience shorthand for {@link SessionData#require(Class, SessionSerializer)}.
   *
   * @param type the type
   * @param serializer the serializer
   * @param <T> the type
   * @return the value for the given key
   */
  default <T> Promise<T> require(Class<T> type, SessionSerializer serializer) {
    return getData().map(d -> d.require(type, serializer));
  }

  /**
   * A convenience shorthand for {@link SessionData#require(String)}.
   *
   * @param name the object name
   * @return the value for the given key
   */
  default Promise<?> require(String name) {
    return getData().map(d -> d.require(name));
  }

  /**
   * A convenience shorthand for {@link SessionData#require(String, SessionSerializer)}.
   *
   * @param name the object name
   * @param serializer the serializer
   * @return the value for the given key
   */
  default Promise<?> require(String name, SessionSerializer serializer) {
    return getData().map(d -> d.require(name, serializer));
  }

  /**
   * A convenience shorthand for {@link SessionData#set(SessionKey, Object)}.
   *
   * @param key the key
   * @param value the value
   * @param <T> the type
   * @return the operation for setting the value
   */
  default <T> Operation set(SessionKey<T> key, T value) {
    return getData().operation(d -> d.set(key, value));
  }

  /**
   * A convenience shorthand for {@link SessionData#set(SessionKey, Object, SessionSerializer)}.
   *
   * @param key the key
   * @param value the value
   * @param serializer the serializer
   * @param <T> the type
   * @return the operation for setting the value
   */
  default <T> Operation set(SessionKey<T> key, T value, SessionSerializer serializer) {
    return getData().operation(d -> d.set(key, value, serializer));
  }

  /**
   * A convenience shorthand for {@link SessionData#set(Class, Object)}.
   *
   * @param type the type
   * @param value the value
   * @param <T> the type
   * @return the operation for setting the value
   */
  default <T> Operation set(Class<T> type, T value) {
    return getData().operation(d -> d.set(type, value));
  }

  /**
   * A convenience shorthand for {@link SessionData#set(Class, Object, SessionSerializer)}.
   *
   * @param type the type
   * @param value the value
   * @param serializer the serializer
   * @param <T> the type
   * @return the operation for setting the value
   */
  default <T> Operation set(Class<T> type, T value, SessionSerializer serializer) {
    return getData().operation(d -> d.set(type, value, serializer));
  }

  /**
   * A convenience shorthand for {@link SessionData#set(String, Object)}.
   *
   * @param name the name
   * @param value the value
   * @param <T> the type
   * @return the operation for setting the value
   */
  default <T> Operation set(String name, T value) {
    return getData().operation(d -> d.set(name, value));
  }

  /**
   * A convenience shorthand for {@link SessionData#set(String, Object, SessionSerializer)}.
   *
   * @param name the name
   * @param value the value
   * @param serializer the serializer
   * @param <T> the type
   * @return the operation for setting the value
   */
  default <T> Operation set(String name, T value, SessionSerializer serializer) {
    return getData().operation(d -> d.set(name, value, serializer));
  }

  /**
   * A convenience shorthand for {@link SessionData#set(Object)}.
   *
   * @param value the value
   * @param <T> the type
   * @return the operation for setting the value
   */
  default <T> Operation set(T value) {
    return getData().operation(d -> d.set(value));
  }

  /**
   * A convenience shorthand for {@link SessionData#set(Object, SessionSerializer)}.
   *
   * @param value the value
   * @param serializer the serializer
   * @param <T> the type
   * @return the operation for setting the value
   */
  default <T> Operation set(T value, SessionSerializer serializer) {
    return getData().operation(d -> d.set(value, serializer));
  }

  /**
   * A convenience shorthand for {@link SessionData#remove(SessionKey)}.
   *
   * @param key the key
   * @return the operation for removing the value
   */
  default Operation remove(SessionKey<?> key) {
    return getData().operation(d -> d.remove(key));
  }

  /**
   * A convenience shorthand for {@link SessionData#remove(Class)}.
   *
   * @param type the type
   * @return the operation for removing the value
   */
  default Operation remove(Class<?> type) {
    return getData().operation(d -> d.remove(type));
  }

  /**
   * A convenience shorthand for {@link SessionData#remove(String)}.
   *
   * @param name the name
   * @return the operation for removing the value
   */
  default Operation remove(String name) {
    return getData().operation(d -> d.remove(name));
  }

  /**
   * A convenience shorthand for {@link SessionData#clear()}.
   *
   * @return the operation for clearing the session
   */
  default Operation clear() {
    return getData().operation(SessionData::clear);
  }

  /**
   * Whether or not any changes have been made to the session data since it was accessed.
   *
   * @return whether or not any changes have been made to the session data since it was accessed
   */
  boolean isDirty();

  /**
   * Persists the session data.
   * <p>
   * It is generally not necessary to call this method explicitly.
   * The {@link SessionModule} installs a {@link Response#beforeSend(Action) response finalizer} that will
   * call this method if the session {@link #isDirty() is dirty}.
   * <p>
   * This method is effectively a noop if the session data has not yet been accessed.
   * <p>
   * If the session data has been accessed, calling this method will initiate a store of the data regardless of whether the data is dirty or not.
   * <p>
   * The {@link #isDirty()} will always return {@code true} after calling this method, until changes are made to the session data.
   *
   * @return the save operation
   */
  Operation save();

  /**
   * Terminates the session and session id.
   * <p>
   * Calling this method will immediately reset the state of the session data, and initiate a call to {@link SessionStore#remove(AsciiString)} on the underlying store.
   * This effectively resets the state of this object.
   * <p>
   * This method also invokes the {@link SessionId#terminate()} method, which prevents the same ID from being used subsequently.
   *
   * @return the terminate operation
   */
  Operation terminate();

  /**
   * The value serializer that is guaranteed to be able to serialize/deserialize any Java object that implements {@link java.io.Serializable}.
   * <p>
   * Ratpack extensions, libraries etc. should explicitly use this serializer (e.g. with {@link SessionData#set(SessionKey, Object, SessionSerializer)}
   * when reading and writing to the session as it is not guaranteed that the {@link #getDefaultSerializer() default serializer} relies on Java serialization.
   *
   * @return a serializer for {@link Serializable} objects
   */
  JavaSessionSerializer getJavaSerializer();

  /**
   * The serializer that is used when a serializer is not explicitly given.
   * <p>
   * The default configuration of {@link SessionModule} configures this serializer to be the same as the {@link #getJavaSerializer() Java serializer}.
   * However, if you'd prefer to use a different serialization strategy by default in your application you can override this binding.
   *
   * @return the serializer to use by default
   */
  SessionSerializer getDefaultSerializer();
}
