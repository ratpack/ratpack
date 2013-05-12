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

package org.ratpackframework.util;

/**
 * An object that transforms an object into another.
 *
 * @param <F> The type of the input object.
 * @param <T> The type of the output (transformed) object.
 */
public interface Transformer<F, T> {

  /**
   * Transforms the given object into a different object.
   *
   * @param from The object to transform
   * @return The transformed object
   */
  T transform(F from);

}
