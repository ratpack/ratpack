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

package ratpack.session.store

import com.google.common.base.Charsets
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.util.AsciiString
import ratpack.server.StartEvent
import ratpack.server.StopEvent
import ratpack.test.exec.ExecHarness
import spock.lang.Specification

public class RedisSessionStoreSpec extends Specification {


  ExecHarness harness

  def setup() {
    harness = ExecHarness.harness()
  }

  def cleanup() {
    harness.close()
  }

  def "Create valid connection string with default config"() {
    given:
    def config = new RedisSessionModule.Config()
    def sessionStore = new RedisSessionStore(config)

    when:
    def connectionString = sessionStore.getConnectionString()

    then:
    connectionString == "redis://127.0.0.1"
  }


  def "store and load a session in redis"() {
    given:
    def config = new RedisSessionModule.Config()
    def sessionStore = new RedisSessionStore(config)
    def sessionId = new AsciiString("fakeSessionId")

    ByteBufAllocator bufAllocator = new UnpooledByteBufAllocator(false)
    and:
    sessionStore.onStart(Mock(StartEvent))

    when:
    harness.execute(sessionStore.store(sessionId, bufAllocator.buffer().writeBytes("data".getBytes())))

    and:
    def result = harness.yield {
      return sessionStore.load(sessionId)
    }

    then:
    result.getValueOrThrow().toString(Charsets.UTF_8) == "data"

    when:
    harness.execute(sessionStore.remove(sessionId))

    and:
    result = harness.yield {
      return sessionStore.load(sessionId)
    }

    then:
    result.getValueOrThrow().toString(Charsets.UTF_8) == ""


    cleanup:
    sessionStore.onStop(Mock(StopEvent))

  }

}
