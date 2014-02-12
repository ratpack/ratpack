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

package ratpack.thymeleaf

import org.thymeleaf.Arguments
import org.thymeleaf.dom.Element
import org.thymeleaf.processor.attr.AbstractTextChildModifierAttrProcessor

class SayToAttrProcessor extends AbstractTextChildModifierAttrProcessor {
  int precedence = 10000
  SayToAttrProcessor() {
    super('sayto')
  }
  protected String getText(Arguments arguments, Element element, String attributeName) {
    return "Hello, ${element.getAttributeValue(attributeName)}!"
  }
}
