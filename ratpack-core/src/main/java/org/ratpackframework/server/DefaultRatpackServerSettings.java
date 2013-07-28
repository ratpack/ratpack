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

package org.ratpackframework.server;

import java.io.File;

/**
 * A default implementation of {@link RatpackServerSettings}.
 */
public class DefaultRatpackServerSettings implements RatpackServerSettings {

  private final File baseDir;
  private final boolean reloadable;

  /**
   * Constructor.
   *
   * @param baseDir The base dir value.
   * @param reloadable The reloadable value.
   */
  public DefaultRatpackServerSettings(File baseDir, boolean reloadable) {
    this.baseDir = baseDir;
    this.reloadable = reloadable;
  }

  public File getBaseDir() {
    return baseDir;
  }

  public boolean isReloadable() {
    return reloadable;
  }
}
