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

package org.ratpackframework.groovy.templating.internal;

import org.ratpackframework.groovy.templating.TemplatingConfig;

public class DefaultTemplatingConfig implements TemplatingConfig {

  private final String templatesPath;
  private final int cacheSize;
  private final boolean checkTimeStamp;
  private final boolean staticallyCompile;

  public DefaultTemplatingConfig(String templatesPath, int cacheSize, boolean checkTimeStamp, boolean staticallyCompile) {
    this.templatesPath = templatesPath;
    this.cacheSize = cacheSize;
    this.checkTimeStamp = checkTimeStamp;
    this.staticallyCompile = staticallyCompile;
  }

  public int getCacheSize() {
    return cacheSize;
  }

  public boolean isCheckTimestamp() {
    return checkTimeStamp;
  }

  public boolean isStaticallyCompile() {
    return staticallyCompile;
  }

  public String getTemplatesPath() {
    return templatesPath;
  }

}
