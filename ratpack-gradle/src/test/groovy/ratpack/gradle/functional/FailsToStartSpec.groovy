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

package ratpack.gradle.functional

class FailsToStartSpec extends FunctionalSpec {

  def "process does not hang if it fails to start"() {
    when:
    buildFile << """
      configureRun.doLast { run.systemProperty "ratpack.reloadable", false }
    """

    file("src/ratpack/ratpack.properties") << "handlerFactory=test.HandlerFactory"
    file("src/main/java/test/HandlerFactory.java") << """
      package test;

      import ratpack.launch.*;
      import ratpack.handling.*;

      public class HandlerFactory implements ratpack.launch.HandlerFactory {
        public Handler create(LaunchConfig launchConfig) {
          throw new RuntimeException("bang!");
        }
      }
    """

    then:
    run("run").failure
  }

}
