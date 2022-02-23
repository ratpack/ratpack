/*
 * Copyright 2022 the original author or authors.
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

package ratpack.test.docs.executer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ExceptionTransformer {

  private final String sourceClassName;
  private final String sourceFileName;
  private final Integer lineNumber;

  public ExceptionTransformer(String sourceClassName, String sourceFileName, Integer lineNumber) {
    this.sourceClassName = sourceClassName;
    this.sourceFileName = sourceFileName;
    this.lineNumber = lineNumber;
  }

  public Throwable transform(Throwable throwable, Integer offset) throws Exception {
    int errorLine = 0;

    if (throwable instanceof CompileException) {
      CompileException e = (CompileException) throwable;
      errorLine = e.getLineNo();
    } else {
      Optional<StackTraceElement> frame = Arrays.stream(throwable.getStackTrace()).filter(f -> f.getFileName().equals(sourceClassName)).findFirst();
      if (frame.isPresent()) {
        errorLine = frame.get().getLineNumber();
      } else {
        frame = Arrays.stream(throwable.getStackTrace()).filter(f -> f.getFileName().equals("Example.java")).findFirst();
        if (frame.isPresent()) {
          errorLine = frame.get().getLineNumber();
        }
      }
    }
    errorLine = errorLine - offset;
    StackTraceElement[] stack = throwable.getStackTrace();
    List<StackTraceElement> newStack = new ArrayList<>(stack.length + 1);
    newStack.add(new StackTraceElement(sourceClassName, "javadoc", sourceFileName, lineNumber + errorLine));
    newStack.addAll(Arrays.asList(stack));
    throwable.setStackTrace(newStack.toArray(new StackTraceElement[]{}));
    return throwable;
  }
}
