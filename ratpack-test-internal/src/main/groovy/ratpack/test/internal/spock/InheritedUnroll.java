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

/**
 * Annotation analogous in behavior to {@link spock.lang.Unroll}, but
 * in addition to the original annotation, this annotation is inherited.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
@ExtensionAnnotation(InheritedUnrollExtension.class)
public @interface InheritedUnroll {

  String value() default "";

}
