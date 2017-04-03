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

package ratpack.util;

import com.google.common.io.Resources;
import io.netty.util.CharsetUtil;

import java.net.URL;

import static ratpack.util.Exceptions.uncheck;

/**
 * Provides the version of the Ratpack core at runtime.
 */
public class RatpackVersion {

  private static final String RESOURCE_PATH = "ratpack/ratpack-version.txt";

  private RatpackVersion() {
  }

  /**
   * The version of Ratpack.
   *
   * @return The version of Ratpack
   */
  public static String getVersion() {
    ClassLoader classLoader = RatpackVersion.class.getClassLoader();
    URL resource = classLoader.getResource(RESOURCE_PATH);
    if (resource == null) {
      throw new RuntimeException("Could not find " + RESOURCE_PATH + " on classpath");
    }

    try {
      return Resources.toString(resource, CharsetUtil.UTF_8).trim();
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

}
