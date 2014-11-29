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

package ratpack.session;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface Crypto {

  /**
   * Signs a message using the application's secret key
   *
   * @param message The message to sign
   * @return The message signed with the application's secret key
   * @throws UnsupportedEncodingException
   * @throws InvalidKeyException
   * @throws NoSuchAlgorithmException
   */
  public String sign(String message) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException;


  /**
   * Signs a message using a given secret
   *
   * @param message The message to sign
   * @param key The secret used to sign the message
   * @return
   * @throws UnsupportedEncodingException
   * @throws InvalidKeyException
   * @throws NoSuchAlgorithmException
   */
  public String sign(String message, byte[] key) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException;

}
