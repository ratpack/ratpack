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

package ratpack.groovy.test.embed;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.test.embed.BaseDirBuilder;
import ratpack.test.embed.EmbeddedApplication;

/**
 * Static factory methods for creating {@link EmbeddedApplication} objects.
 */
public abstract class EmbeddedApplications {

  private EmbeddedApplications() { }

  /**
   * Constructs a closure backed embedded application with no base dir.
   * @param config the definition of the application
   * @return an embedded application
   *
   * @see ratpack.groovy.test.embed.ClosureBackedEmbeddedApplication
   */
  public static EmbeddedApplication embeddedApp(@DelegatesTo(value = ClosureBackedEmbeddedApplication.class, strategy = Closure.DELEGATE_FIRST) Closure<?> config) {
    return embeddedApp(null, config);
  }

  /**
   * Constructs a closure backed embedded application with a base dir.
   * @param baseDir the builder whose {@link BaseDirBuilder#build()} method will be called to provide the base dir for this app
   * @param config the definition of the application
   * @return
   */
  public static EmbeddedApplication embeddedApp(BaseDirBuilder baseDir, @DelegatesTo(value = ClosureBackedEmbeddedApplication.class, strategy = Closure.DELEGATE_FIRST) Closure<?> config) {
    ClosureBackedEmbeddedApplication app;
    if (baseDir == null) {
      app = new ClosureBackedEmbeddedApplication();
    } else {
      app = new ClosureBackedEmbeddedApplication(baseDir);
    }
    ClosureUtil.configureDelegateFirst(app, config);
    return app;
  }

}
