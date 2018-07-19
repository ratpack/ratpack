/*
 * Copyright 2018 the original author or authors.
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

package ratpack.test.http;

public interface MultipartFormSpec {

  /**
   * Add a field to the multipart form
   * <p>
   * Note: Can be called multiple times to associate more than one value with a given field.
   *
   * @param name the field name
   * @param value the field value
   * @return a specification of a multipart form
   */
  MultipartFormSpec field(String name, String value);

  /**
   * A specification of a file to upload (see RFC2388)
   * <p>
   * Can be used to construct a multipart form with files
   *
   * @return a specification of a multipart file
   */
  MultipartFileSpec file();

}
