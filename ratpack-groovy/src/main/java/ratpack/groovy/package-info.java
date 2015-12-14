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

/**
 * Support for writing Ratpack applications in the <a href="http://beta.groovy-lang.org">Groovy programming language</a>.
 * <h3>Key Features</h3>
 * <p>The Groovy support provides the following key features:</p>
 * <ul>
 * <li>A {@link groovy.lang.Closure} based Handler Chain building DSL - see {@link ratpack.groovy.handling.GroovyChain}.</li>
 * <li>Dynamic templates based on embedded groovy code - see {@link ratpack.groovy.template.TextTemplateModule}.</li>
 * <li>Defining applications as a single script - see {@link ratpack.groovy.Groovy.Ratpack}.</li>
 * </ul>
 * <h3>Use of <a href="http://beta.groovy-lang.org/closures.html">closures</a> and {@link groovy.lang.DelegatesTo}</h3>
 * <p>
 * Ratpack makes heavy use of a feature of Groovy 2.1 that allows the delegate of a closure to be specified via the type system.
 * Namely, the {@link groovy.lang.DelegatesTo} annotation.
 * On all parts of the API where a {@link groovy.lang.Closure} is being accepted as a method parameter, the parameter will be
 * annotated with this annotation. This specifies the type of the closure <i>delegate</i> object during execution.
 * For more information on closure delegates, see <a href="http://mrhaki.blogspot.co.uk/2009/11/groovy-goodness-setting-closures.html">this article</a>.
 * <p>
 * When reading the API reference, it's important to take note of the {@link groovy.lang.DelegatesTo} annotation on closure parameters.
 * This parameter indicates what type to look at for information on what the closure can do.
 * </p>
 */
package ratpack.groovy;
