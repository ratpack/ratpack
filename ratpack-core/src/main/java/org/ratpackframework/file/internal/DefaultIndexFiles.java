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

package org.ratpackframework.file.internal;

import org.ratpackframework.file.IndexFiles;
import org.ratpackframework.launch.LaunchConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A default implementation of {@link IndexFiles}.
 */
public class DefaultIndexFiles implements IndexFiles {

  public static final String LAUNCH_CONFIG_PROPERTY_NAME = "other.indexFiles";

  private List<String> fileNames = new ArrayList<>();

  /**
   * Constructor that takes in a comma separated string of file names.
   *
   * @param indexFiles a comma separated string of file names
   */
  public DefaultIndexFiles(String indexFiles) {
    if (indexFiles != null) {
      fileNames.addAll(Arrays.asList(indexFiles.split(",")));
    }
  }

  /**
   * Constructor that takes in a string array of file names.
   *
   * @param indexFiles a string array of file names
   */
  public DefaultIndexFiles(String[] indexFiles) {
    fileNames.addAll(Arrays.asList(indexFiles));
  }

  public List<String> getFileNames() {
    return fileNames;
  }

  /**
   * Create an implementation based on the value of the {@value #LAUNCH_CONFIG_PROPERTY_NAME} launch config property.
   *
   * @param launchConfig The launch config
   * @return An IndexFiles
   */
  public static IndexFiles indexFiles(LaunchConfig launchConfig) {
    return new DefaultIndexFiles(launchConfig.getOther(LAUNCH_CONFIG_PROPERTY_NAME, null));
  }

}
