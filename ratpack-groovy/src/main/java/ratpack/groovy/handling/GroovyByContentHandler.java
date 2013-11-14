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

package ratpack.groovy.handling;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.handling.ByContentHandler;

public interface GroovyByContentHandler extends ByContentHandler {

  GroovyByContentHandler type(String mimeType, @DelegatesTo(GroovyContext.class) Closure<?> closure);

  GroovyByContentHandler plainText(@DelegatesTo(GroovyContext.class) Closure<?> closure);

  GroovyByContentHandler html(@DelegatesTo(GroovyContext.class) Closure<?> closure);

  GroovyByContentHandler json(@DelegatesTo(GroovyContext.class) Closure<?> closure);

  GroovyByContentHandler xml(@DelegatesTo(GroovyContext.class) Closure<?> closure);

}
