/*
 * Copyright 2016 the original author or authors.
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

package ratpack.session.clientside.internal

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import ratpack.session.clientside.ClientSideSessionConfig
import ratpack.test.internal.TestByteBufAllocators
import spock.lang.Specification

import static ratpack.session.clientside.ClientSideSessionSpec.SUPPORTED_ALGORITHMS
import static ratpack.session.clientside.ClientSideSessionSpec.keyLength

class DefaultCryptoSpec extends Specification {

  def "can roundtrip data with #algorithm"() {
    given:
    def config = new ClientSideSessionConfig(secretKey: "a" * keyLength(algorithm), cipherAlgorithm: algorithm)
    def crytpo = new DefaultCrypto(config.getSecretKey().getBytes(CharsetUtil.UTF_8), config.getCipherAlgorithm())

    // in this branch, NoPadding algorithms don't support ending the data with zero - it gets chopped off
    def zeroAllowed = !algorithm.endsWith("/NoPadding")

    when:
    for (int len = 1; len < 65; len++) {
      for (int i = 0; i < 50; i++) {
        byte[] plaintextBytes = randomBytes(len, zeroAllowed)
        def plaintext = Unpooled.wrappedBuffer(plaintextBytes)
        def encrypted = crytpo.encrypt(plaintext, TestByteBufAllocators.LEAKING_UNPOOLED_HEAP)
        def decrypted = crytpo.decrypt(encrypted, TestByteBufAllocators.LEAKING_UNPOOLED_HEAP)
        def decryptedBytes = ByteBufUtil.getBytes(decrypted)
        assert Arrays.equals(plaintextBytes, decryptedBytes)
      }
    }

    then:
    true

    where:
    algorithm << SUPPORTED_ALGORITHMS

  }

  private static byte[] randomBytes(int len, boolean zeroAllowed) {
    Random random = new Random()
    byte[] array = new byte[len]
    random.nextBytes(array)
    if (!zeroAllowed) {
      while (array[len - 1] == (byte) 0) {
        array[len - 1] = random.nextInt(127)
      }
    }
    array
  }

}


