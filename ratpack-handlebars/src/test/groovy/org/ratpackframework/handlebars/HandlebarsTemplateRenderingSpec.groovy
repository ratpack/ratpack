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

package org.ratpackframework.handlebars

import org.ratpackframework.test.groovy.RatpackGroovyDslSpec

import static org.ratpackframework.handlebars.HandlebarsTemplate.handlebarsTemplate

class HandlebarsTemplateRenderingSpec extends RatpackGroovyDslSpec {

  def setup() {
    modules << new HandlebarsModule()
  }

  void 'can render a handlebars template from default location'() {
    given:
    file('handlebars/simple.hbs') << '{{key}}'

    when:
    app {
      handlers {
        get {
          render handlebarsTemplate('simple', key: 'it works!')
        }
      }
    }

    then:
    text == 'it works!'
  }
}
