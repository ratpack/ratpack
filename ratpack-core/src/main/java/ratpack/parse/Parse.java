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

package ratpack.parse;

/**
 * A parse object is a description of how to parse the request into an object.
 * <p>
 * The minimum requirement of a parse object is to specify what the object type should be.
 * Specialisations of this type may provide more information that can be used in deserialization.
 * <p>
 * Importantly, parse objects are agnostic to the type of the request body.
 * Different {@link Parser} implementations are responsible for converting from different mime types to object types.
 * <p>
 * For no option parse objects, use {@link NoOptParse}.
 * To implement a custom parse type with objects, see {@link ParseSupport}
 *
 * @param <T> the type of object to parse the request into
 * @see Parser
 * @see NoOptParse
 * @see ParserSupport
 */
public interface Parse<T> {

  /**
   * The type of object to parse to.
   *
   * @return the type of object to parse to
   */
  Class<T> getType();

}
