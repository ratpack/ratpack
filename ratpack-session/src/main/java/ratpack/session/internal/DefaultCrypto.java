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

package ratpack.session.internal;

import com.google.common.io.BaseEncoding;
import ratpack.session.Crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class DefaultCrypto implements Crypto {

  private final String secret;

  public DefaultCrypto() {
    this.secret = Long.toString(System.nanoTime());
  }

  @Override
  public String sign(String message) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
    return sign(message, secret.getBytes("utf-8"));
  }

  @Override
  public String sign(String message, byte[] key) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
    SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA1");
    Mac mac = Mac.getInstance("HmacSHA1");
    mac.init(secretKeySpec);
    byte[] signed = mac.doFinal(message.getBytes("utf-8"));
    return BaseEncoding.base64Url().encode(signed);
  }

}
