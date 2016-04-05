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

package ratpack.service;

import java.lang.annotation.*;

/**
 * Declares the other service types that services of the annotated type depend on.
 * <p>
 * This annotation is only effective when present on {@link Service} types.
 *
 * @see ServiceDependencies
 * @since 1.3
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface DependsOn {

  /**
   * The types of services that services of the annotated type depend on.
   *
   * @return the types of services that services of the annotated type depend on.
   */
  Class<?>[] value();

}
