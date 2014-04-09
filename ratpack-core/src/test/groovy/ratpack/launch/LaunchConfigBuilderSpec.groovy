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

package ratpack.launch

import ratpack.handling.Context
import ratpack.handling.Handler
import spock.lang.Specification

class LaunchConfigBuilderSpec extends Specification {

  static class TestHandlerFactory implements HandlerFactory {
    @Override
    Handler create(LaunchConfig launchConfig) {
      new Handler() {
        void handle(Context context) throws Exception { }
      }
    }
  }

  def "no base dir"() {
    when:
    LaunchConfigBuilder.noBaseDir().build(new TestHandlerFactory()).baseDir

    then:
    thrown(NoBaseDirException)
  }

}
