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

public interface MultipartFileSpec {

  /**
   * Add the file to the parent multipart form
   * <p>
   * Note: Must be called to include the file into the payload.
   *
   * @return a specification of a multipart form
   */
  MultipartFormSpec add();

  /**
   * Specify the content type
   * <p>
   * Required. Defaults to 'text/plain'.
   *
   * @return a specification of a multipart file
   */
  MultipartFileSpec contentType(String contentType);

  /**
   * Specify the file contents
   * <p>
   * Required. No default provided.
   *
   * @return a specification of a multipart file
   */
  MultipartFileSpec data(byte[] data);

  /**
   * Specify the file contents
   * <p>
   * Required. No default provided.
   * <p>
   * Note: this value is encoded into a byte array using UTF-8
   *
   * @return a specification of a multipart file
   */
  MultipartFileSpec data(String data);

  /**
   * Specify the form field
   * <p>
   * Optional. No default provided.
   * <p>
   * Valid options include BASE64, QUOTED-PRINTABLE, 8BIT, 7BIT, BINARY.
   * Custom encodings of the for X-<encodingname> are also supported.
   * See https://www.w3.org/Protocols/rfc1341/5_Content-Transfer-Encoding.html for more information.
   * <p>
   * Note: Netty will need to understand this encoding scheme.
   *
   * @return a specification of a multipart file
   */
  MultipartFileSpec encoding(String encoding);

  /**
   * Specify the form field
   * <p>
   * Required. Defaults to 'file'.
   *
   * @return a specification of a multipart file
   */
  MultipartFileSpec field(String field);

  /**
   * Specify the uploaded filename
   * <p>
   * Required. Defaults to 'filename.txt'.
   *
   * @return a specification of a multipart file
   */
  MultipartFileSpec name(String name);

}
