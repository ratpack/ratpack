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
   * The session data.
   * <p>
   * The data is available via a promise to support backing {@link SessionStore} implementations that load the data asynchronously.
   *
   * @return the session data
   */
  Promise<SessionData> getData();

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
