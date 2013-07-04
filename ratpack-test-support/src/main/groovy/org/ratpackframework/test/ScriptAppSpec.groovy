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

package org.ratpackframework.test

import org.ratpackframework.bootstrap.RatpackServer
import org.ratpackframework.groovy.bootstrap.RatpackScriptApp


class ScriptAppSpec extends RequestingSpec {

  @Override
  protected RatpackServer createServer() {

    return RatpackScriptApp.ratpack(script, baseDir, port, address, compileStatic, reloadable)
  }

  protected File getScript(){
    //TODO Confirm default location of ratpack.groovy
    return new File("src/ratpack/ratpack.groovy")
  }

  protected File getBaseDir(){
    return script.getAbsoluteFile().getParentFile()
  }

  protected int getPort(){
    return 0
  }

  protected InetAddress getAddress(){
    return null
  }

  protected boolean getCompileStatic(){
    return false
  }

  protected boolean getReloadable(){
    return false
  }

}
