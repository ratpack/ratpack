/*
 * Copyright 2012 the original author or authors.
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

package com.timberglund.ratpack.gradle

import spock.lang.Specification
import org.junit.rules.TemporaryFolder
import org.junit.Rule

public class RatpackPluginMetaTest extends Specification {

  @Rule TemporaryFolder tmp
  
  private tmpFileCounter = 0
  
  def "can load when file has not been parameterised"() {
    given:
    def loader = createMockedClassLoader """
      ratpack-version=\${ratpackVersion}
    """ 
    
    when:
    def meta = RatpackPluginMeta.fromResource(loader)
    
    then:
    meta.ratpackVersion == RatpackPluginMeta.DEFAULT_RATPACK_VERSION
  }

  def "can load when file has been parameterised"() {
    given:
    def loader = createMockedClassLoader """
      ratpack-version=1.3
    """

    when:
    def meta = RatpackPluginMeta.fromResource(loader)

    then:
    meta.ratpackVersion == "1.3"
  }
  
  def ClassLoader createMockedClassLoader(String content) {
    def loader = Mock(ClassLoader)
    interaction { _ * loader.getResource("ratpack.properties") >> createUrl(content) }
    loader
  }
  
  def URL createUrl(String content) {
    def file = tmp.newFile("tmp${tmpFileCounter++}.txt")
    file.write(content)
    file.toURL()
  }
}
