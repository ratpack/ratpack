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

package ratpack.groovy.template.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.CharsetUtil;
import org.codehaus.groovy.control.CompilationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.groovy.script.internal.ScriptEngine;

import java.io.IOException;

public class TextTemplateCompiler {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ByteBufAllocator byteBufAllocator;

  private boolean verbose;
  private final TextTemplateParser parser = new TextTemplateParser();
  private final ScriptEngine<DefaultTextTemplateScript> scriptEngine;

  public TextTemplateCompiler(ScriptEngine<DefaultTextTemplateScript> scriptEngine, ByteBufAllocator byteBufAllocator) {
    this(scriptEngine, false, byteBufAllocator);
  }

  public TextTemplateCompiler(ScriptEngine<DefaultTextTemplateScript> scriptEngine, boolean verbose, ByteBufAllocator byteBufAllocator) {
    this.scriptEngine = scriptEngine;
    this.verbose = verbose;
    this.byteBufAllocator = byteBufAllocator;
  }

  public CompiledTextTemplate compile(ByteBuf templateSource, String name) throws CompilationFailedException, IOException {
    ByteBuf scriptSource = byteBufAllocator.buffer(templateSource.capacity());
    parser.parse(templateSource, scriptSource);

    String scriptSourceString = scriptSource.toString(CharsetUtil.UTF_8);
    scriptSource.release();

    if (verbose && logger.isInfoEnabled()) {
      logger.info("\n-- script source --\n" + scriptSourceString + "\n-- script end --\n");
    }

    try {
      Class<DefaultTextTemplateScript> scriptClass = scriptEngine.compile(name, scriptSourceString);
      return new CompiledTextTemplate(name, scriptClass);
    } catch (Exception e) {
      throw new InvalidTemplateException(name, "compilation failure", e);
    }
  }

}
