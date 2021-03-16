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

import org.slf4j.LoggerFactory;
import ratpack.session.internal.UnsafeAllowAllSessionTypeFilter;

import java.io.NotSerializableException;

/**
 * A filter that determines whether a type is safe for session usage.
 * <p>
 * Most applications should not need to use or implement this interface.
 * By default, an implementation is used that allows:
 * <ul>
 * <li>types annotated with {@link AllowedSessionType}</li>
 * <li>types from {@code java.lang} or {@code java.util}</li>
 * <li>{@link NotSerializableException} and its super types</li>
 * <li>types registered with {@link SessionModule#allowTypes}</li>
 * <li>types allowed by any multi-bound {@link SessionTypeFilterPlugin} implementations</li>
 * </ul>
 * If this strategy is unsuitable, a custom implementation of this interface may be provided.
 *
 * @since 1.9
 */
public interface SessionTypeFilter {

  /**
   * Indicates whether the given type is allowed to be stored or loaded from session data.
   *
   * @param type the type in question
   * @return whether or the not type can be used
   */
  boolean allow(Class<?> type);

  /**
   * Throws {@link NonAllowedSessionTypeException} if the given type is not allowed by this filterer.
   * <p>
   * This method is typically called by {@link SessionSerializer} implementations before serializing or deserializing a type.
   * It does not need to be overridden by implementations of this type.
   *
   * @param type the type to assert
   */
  default void assertAllowed(Class<?> type) throws NonAllowedSessionTypeException {
    if (!allow(type)) {
      throw new NonAllowedSessionTypeException(type);
    }
  }

  /**
   * An unsafe implementation that allows all types.
   * <p>
   * This implementation is unsafe in that it allows <a href="https://portswigger.net/web-security/deserialization">“gadget attacks”</a>
   * if session payloads can be forged.
   * It should only be used as a temporary measure.
   *
   * @return a session type filter that allows all types.
   * @see SessionModule#allowTypes
   * @since 1.9
   * @deprecated since 1.9
   */
  @Deprecated
  static SessionTypeFilter unsafeAllowAll() {
    LoggerFactory.getLogger(SessionTypeFilter.class).warn("SessionTypeFilter.unsafeAllowAll() used which is a security risk due to insecure deserialization. Please consult documentation for SessionModule.");
    return new UnsafeAllowAllSessionTypeFilter();
  }

}
