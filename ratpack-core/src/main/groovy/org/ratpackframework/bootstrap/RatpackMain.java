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

import com.google.inject.Module;
import org.ratpackframework.bootstrap.internal.RootModule;
import org.ratpackframework.config.Config;
import org.ratpackframework.config.internal.ConfigLoader;
import org.ratpackframework.config.internal.ConfigModule;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class RatpackMain {

  public static void main(String[] args) throws Exception {
    File configFile = args.length == 0 ? new File("config.groovy") : new File(args[0]);
    if (!configFile.exists() && args.length > 0) {
      System.err.println("Config file $configFile.absolutePath does not exist");
      System.exit(1);
    }

    new RatpackServerFactory().create(new ConfigLoader().load(configFile)).startAndWait();
  }

}
