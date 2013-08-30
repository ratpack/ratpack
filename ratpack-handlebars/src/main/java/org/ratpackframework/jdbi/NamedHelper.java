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

package org.ratpackframework.jdbi;

import com.github.jknack.handlebars.Helper;

/**
 * Instances of classes implementing this interface bound to the module registry will be automatically
 * registered as handlebars helpers.
 * <p/>
 * Example usage: (Java DSL)
 * <pre class="tested">
 * import org.ratpackframework.handlebars.NamedHelper;
 * import com.github.jknack.handlebars.Options;
 * import org.ratpackframework.handling.*;
 * import org.ratpackframework.guice.*;
 * import org.ratpackframework.util.*;
 * import org.ratpackframework.launch.*;
 *
 * public class MultiplyHelper implements NamedHelper<String> {
 *
 *   public String getName() {
 *     return "hello";
 *   }
 *
 *   CharSequence apply(String context, Options options) throws IOException {
 *     return String.format("Hello %s", context)
 *   }
 * }
 *
 * class ModuleBootstrap implements Action&lt;ModuleRegistry&gt; {
 *   public void execute(ModuleRegistry modules) {
 *     modules.bind(MultiplyHelper.class)
 *   }
 * }
 *
 * LaunchConfig launchConfig = LaunchConfigBuilder.baseDir(new File("appRoot"))
 *   .build(new HandlerFactory() {
 *     public Handler create(LaunchConfig launchConfig) {
 *       return Guice.handler(launchConfig, new ModuleBootstrap(), new Action&lt;Chain&gt;() {
 *         public void execute(Chain chain) {
 *         }
 *       });
 *     }
 *   });
 * </pre>
 *
 * Example usage: (Groovy DSL)
 * <pre>
 * modules {
 *   bind MultiplyHelper
 * }
 * </pre>
 */
public interface NamedHelper<T> extends Helper<T> {
  public String getName();
}
