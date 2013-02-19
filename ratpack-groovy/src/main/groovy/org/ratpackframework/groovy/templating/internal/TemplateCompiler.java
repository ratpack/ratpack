/*
 * Copyright 2003-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ratpackframework.groovy.templating.internal;

import org.codehaus.groovy.control.CompilationFailedException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.util.CharsetUtil;
import org.ratpackframework.groovy.script.ScriptEngine;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TemplateCompiler {

  private final Logger logger = Logger.getLogger(getClass().getName());

  private boolean verbose;
  private final TemplateParser parser = new TemplateParser();
  private final ScriptEngine<TemplateScript> scriptEngine;

  public TemplateCompiler(ScriptEngine<TemplateScript> scriptEngine) {
    this(scriptEngine, false);
  }

  public TemplateCompiler(ScriptEngine<TemplateScript> scriptEngine, boolean verbose) {
    this.scriptEngine = scriptEngine;
    this.verbose = verbose;
  }

  public CompiledTemplate compile(ChannelBuffer templateSource, String name) throws CompilationFailedException, IOException {
    ChannelBuffer scriptSource = ChannelBuffers.dynamicBuffer(templateSource.capacity());
    parser.parse(templateSource, scriptSource);

    String scriptSourceString = scriptSource.toString(CharsetUtil.UTF_8);

    if (verbose && logger.isLoggable(Level.INFO)) {
      logger.info("\n-- script source --\n" + scriptSourceString + "\n-- script end --\n");
    }

    try {
      Class<TemplateScript> scriptClass = scriptEngine.compile(name, scriptSourceString);
      return new CompiledTemplate(name, scriptClass);
    } catch (Exception e) {
      throw new InvalidTemplateException(name, "compilation failure", e);
    }
  }

}
