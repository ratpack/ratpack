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

package ratpack.groovy.launch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.groovy.Groovy;
import ratpack.server.RatpackServer;

import static ratpack.util.ExceptionUtils.uncheck;

/**
 * The standard “main” entry point for Groovy script based apps.
 */
public class GroovyRatpackMain  {

  private final static Logger LOGGER = LoggerFactory.getLogger(GroovyRatpackMain.class);

  public static void main(String[] args) {
    try {
      RatpackServer.of(Groovy.Script.app()).start();
    } catch (Exception e) {
      LOGGER.error("", e);
      throw uncheck(e);
    }
  }
}
