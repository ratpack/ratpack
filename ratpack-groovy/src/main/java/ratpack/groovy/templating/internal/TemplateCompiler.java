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

package ratpack.groovy.templating.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.CharsetUtil;
import org.codehaus.groovy.control.CompilationFailedException;
import ratpack.groovy.script.internal.ScriptEngine;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TemplateCompiler {

  private final Logger logger = Logger.getLogger(getClass().getName());
  private final ByteBufAllocator byteBufAllocator;

  private boolean verbose;
  private final TemplateParser parser = new TemplateParser();
  private final ScriptEngine<DefaultTemplateScript> scriptEngine;

  public TemplateCompiler(ScriptEngine<DefaultTemplateScript> scriptEngine, ByteBufAllocator byteBufAllocator) {
    this(scriptEngine, false, byteBufAllocator);
  }

  public TemplateCompiler(ScriptEngine<DefaultTemplateScript> scriptEngine, boolean verbose, ByteBufAllocator byteBufAllocator) {
    this.scriptEngine = scriptEngine;
    this.verbose = verbose;
    this.byteBufAllocator = byteBufAllocator;
  }

  public CompiledTemplate compile(ByteBuf templateSource, String name) throws CompilationFailedException, IOException {
    ByteBuf scriptSource = byteBufAllocator.buffer(templateSource.capacity());
    parser.parse(templateSource, scriptSource);

    String scriptSourceString = scriptSource.toString(CharsetUtil.UTF_8);
    scriptSource.release();

    if (verbose && logger.isLoggable(Level.INFO)) {
      logger.info("\n-- script source --\n" + scriptSourceString + "\n-- script end --\n");
    }

    try {
      Class<DefaultTemplateScript> scriptClass = scriptEngine.compile(name, scriptSourceString);
      return new CompiledTemplate(name, scriptClass);
    } catch (Exception e) {
      throw new InvalidTemplateException(name, "compilation failure", e);
    }
  }

}
