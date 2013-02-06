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

package org.ratpackframework.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.ratpackframework.bootstrap.internal.DefaultRatpackApp;
import org.ratpackframework.bootstrap.internal.RootModule;
import org.ratpackframework.config.Config;
import org.ratpackframework.config.internal.ConfigModule;

public class RatpackAppFactory {

  public RatpackApp create(Config config) {
    Injector configInjector = Guice.createInjector(new ConfigModule(config));
    Injector appInjector = configInjector.createChildInjector(Modules.override(new RootModule()).with(config.getModules()));
    return appInjector.getInstance(RatpackApp.class);
  }

}
