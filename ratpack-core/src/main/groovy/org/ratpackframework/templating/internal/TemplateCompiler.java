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

package org.ratpackframework.templating.internal;

import groovy.lang.GroovyRuntimeException;
import org.codehaus.groovy.control.CompilationFailedException;
import org.ratpackframework.script.internal.ScriptEngine;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.io.IOException;

public class TemplateCompiler {

  private final Logger logger = LoggerFactory.getLogger(getClass());

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

  public CompiledTemplate compile(Buffer templateSource, String name) throws CompilationFailedException, IOException {
    Buffer scriptSource = new Buffer(templateSource.length());
    parser.parse(templateSource, scriptSource);

    String scriptSourceString = scriptSource.toString();

    if (verbose && logger.isInfoEnabled()) {
      logger.info(new StringBuilder("\n-- script source --\n").append(scriptSourceString).append("\n-- script end --\n"));
    }

    try {
      Class<TemplateScript> scriptClass = scriptEngine.compile(name, scriptSourceString);
      return new CompiledTemplate(name, scriptClass);
    } catch (Exception e) {
      throw new GroovyRuntimeException("Failed to parse template script (your template may contain an error or be trying to use expressions not currently supported): " + e.getMessage());
    }
  }

}
