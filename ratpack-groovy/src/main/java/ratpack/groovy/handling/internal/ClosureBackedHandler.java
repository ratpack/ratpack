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

package ratpack.groovy.handling.internal;

import groovy.lang.Closure;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;
import ratpack.groovy.handling.GroovyContext;
import ratpack.groovy.internal.ClosureInvoker;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.handling.Context;
import ratpack.handling.internal.DescribingHandler;

public class ClosureBackedHandler implements DescribingHandler {

  private final ClosureInvoker<?, GroovyContext> invoker;

  public ClosureBackedHandler(Closure<?> closure) {
    this.invoker = new ClosureInvoker<>(closure);
  }

  public void handle(Context context) {
    invoker.invoke(context, GroovyContext.from(context), Closure.DELEGATE_FIRST);
  }

  @Override
  public void describeTo(StringBuilder stringBuilder) {
    ClosureUtil.SourceInfo sourceInfo = ClosureUtil.getSourceInfo(invoker.getClosure());
    if (sourceInfo == null) {
      ClassPool pool = ClassPool.getDefault();
      try {
        CtClass ctClass = pool.get(invoker.getClosure().getClass().getName());
        CtMethod ctMethod = ctClass.getDeclaredMethod("doCall");
        int lineNumber = ctMethod.getMethodInfo().getLineNumber(0);

        ClassFile classFile = ctClass.getClassFile();
        String sourceFile = classFile.getSourceFile();

        if (lineNumber != -1 && sourceFile != null) {
          stringBuilder
            .append("closure at line ")
            .append(lineNumber)
            .append(" of ")
            .append(sourceFile);
        } else {
          stringBuilder.append("closure ").append(invoker.getClosure().getClass().getName());
        }
      } catch (NotFoundException e) {
        stringBuilder.append(invoker.getClosure().getClass().getName());
      }
    } else {
      stringBuilder
        .append("closure at line ")
        .append(sourceInfo.getLineNumber())
        .append(" of ")
        .append(sourceInfo.getUri());
    }
  }


}
