/*
 * Copyright 2017 the original author or authors.
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

package ratpack.test.internal.spock;

import org.spockframework.runtime.extension.ExtensionAnnotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtensionAnnotation(InheritedTimeoutExtension.class)
@Inherited
public @interface InheritedTimeout {

  /**
   * Returns the duration after which the execution of the annotated feature or fixture
   * method times out.
   *
   * @return the duration after which the execution of the annotated feature or
   * fixture method times out
   */
  int value();

  /**
   * Returns the duration's time unit.
   *
   * @return the duration's time unit
   */
  TimeUnit unit() default TimeUnit.SECONDS;
}
