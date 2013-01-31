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

package org.ratpackframework.app.internal;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.ratpackframework.app.Config;
import org.ratpackframework.script.internal.ScriptEngine;

import java.io.File;

public class ConfigLoader {

  public Config load(File configFile) throws Exception {
    if (!configFile.exists()) {
      return new ConfigScript(configFile.getParentFile());
    } else {
      ScriptEngine<ConfigScript> scriptEngine = new ScriptEngine<>(getClass().getClassLoader(), false, ConfigScript.class);
      return scriptEngine.run(configFile.getName(), ResourceGroovyMethods.getText(configFile), configFile.getParentFile());
    }
  }

}
