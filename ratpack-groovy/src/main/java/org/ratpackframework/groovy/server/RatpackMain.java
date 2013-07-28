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

package org.ratpackframework.groovy.server;

import java.io.File;

/**
 * The standard “main” entry point for Groovy script based apps.
 */
public class RatpackMain {

  private RatpackMain() {}

  /**
   * The name of the default script in the JVM's default directory that will be the script of the application.
   * <p>
   * <b>Value:</b> {@value}
   */
  public static final String SCRIPT_NAME = "ratpack.groovy";

  /**
   * Starts a Groovy script based ratpack app.
   * <h5>Arguments</h5>
   * <p>
   * The first argument may be the path to the Ratpack script (see {@link org.ratpackframework.groovy.RatpackScript}).
   * If omitted, the file name {@value #SCRIPT_NAME} in the JVM's working directory will be used.
   * <h5>Configuration</h5>
   * <p>
   * Further configuration options can be specified by system properties.
   * See {@link RatpackScriptApp.Property} for the available configuration options.
   *
   * @param args The command line args
   * @throws Exception if there is an error starting the application
   */
  public static void main(String[] args) throws Exception {
    File ratpackFile = args.length == 0 ? new File(SCRIPT_NAME) : new File(args[0]);
    if (!ratpackFile.exists()) {
      System.err.println("Ratpack file " + ratpackFile.getAbsolutePath() + " does not exist");
      System.exit(1);
    }


    RatpackScriptApp.ratpack(ratpackFile).start();
  }

}
