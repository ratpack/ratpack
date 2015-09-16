/*
 * Copyright 2014 the original author or authors.
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

package ratpack.handling.internal;

import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;
import ratpack.handling.Handler;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DescribingHandlers {

  public static void describeTo(Handler handler, StringBuilder stringBuilder) {
    Class<? extends Handler> clazz = handler.getClass();
    if (clazz.isAnonymousClass()) {
      ClassPool pool = ClassPool.getDefault();

      CtClass ctClass;
      try {
        ctClass = pool.get(clazz.getName());
        CtBehavior[] behaviors = ctClass.getDeclaredBehaviors();
        List<CtBehavior> withLineNumber =
          Arrays.asList(behaviors).stream()
            .filter(input -> input.getMethodInfo().getLineNumber(0) > 0)
            .sorted((o1, o2) -> Integer.valueOf(o1.getMethodInfo().getLineNumber(0)).compareTo(o2.getMethodInfo().getLineNumber(0)))
            .collect(Collectors.toList());
        if (!withLineNumber.isEmpty()) {
          CtBehavior method = withLineNumber.get(0);
          int lineNumber = method.getMethodInfo().getLineNumber(0);

          ClassFile classFile = ctClass.getClassFile();
          String sourceFile = classFile.getSourceFile();

          if (lineNumber != -1 && sourceFile != null) {
            stringBuilder
              .append("anonymous class ")
              .append(clazz.getName())
              .append(" at approximately line ")
              .append(lineNumber)
              .append(" of ")
              .append(sourceFile);
            return;
          }
        }
      } catch (NotFoundException ignore) {
        // fall through
      }
    }

    stringBuilder.append(clazz.getName());
  }

}

