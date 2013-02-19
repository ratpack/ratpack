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

package org.ratpackframework.groovy.config.internal;

import com.google.inject.Module;
import groovy.lang.Script;
import org.ratpackframework.assets.StaticAssetsConfig;
import org.ratpackframework.config.*;
import org.ratpackframework.groovy.config.Config;
import org.ratpackframework.groovy.app.RoutingConfig;
import org.ratpackframework.groovy.templating.TemplatingConfig;
import org.ratpackframework.session.SessionCookieConfig;

import java.io.File;
import java.util.List;

public class ConfigScript extends Script implements Config {

  private final Config config;

  public ConfigScript(Config config) {
    this.config = config;
  }

  @Override
  public File getBaseDir() {
    return config.getBaseDir();
  }

  @Override
  public AddressConfig getDeployment() {
    return config.getDeployment();
  }

  @Override
  public StaticAssetsConfig getStaticAssets() {
    return config.getStaticAssets();
  }

  @Override
  public TemplatingConfig getTemplating() {
    return config.getTemplating();
  }

  @Override
  public RoutingConfig getRouting() {
    return config.getRouting();
  }

  @Override
  public SessionCookieConfig getSessionCookie() {
    return config.getSessionCookie();
  }

  @Override
  public List<Module> getModules() {
    return config.getModules();
  }

  @Override
  public Object run() {
    return this;
  }

}
