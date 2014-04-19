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

package ratpack.manual.snippets.executer

import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.control.messages.SyntaxErrorMessage
import ratpack.func.Transformer

class ExceptionTransformer implements Transformer<Throwable, Throwable> {

  final String sourceClassName
  final String pre
  final String sourceFileName
  final int lineNumber

  ExceptionTransformer(String sourceClassName, String pre, String sourceFileName, int lineNumber) {
    this.sourceClassName = sourceClassName
    this.pre = pre
    this.sourceFileName = sourceFileName
    this.lineNumber = lineNumber
  }

  @Override
  Throwable transform(Throwable throwable) throws Exception {
    def errorLine = 0

    if (throwable instanceof CompileException) {
        errorLine = throwable.lineNo
    } else {
      def frame = throwable.getStackTrace().find { it.fileName == sourceClassName }
      if (frame) {
        errorLine = frame.lineNumber
      } else {
        frame = throwable.getStackTrace().find { it.fileName == "Example.java" }
        if (frame) {
          errorLine = frame.lineNumber
        }
      }
    }
    errorLine = errorLine - pre.split("\n").size()
    StackTraceElement[] stack = throwable.getStackTrace()
    List<StackTraceElement> newStack = new ArrayList<StackTraceElement>(stack.length + 1)
    newStack.add(new StackTraceElement(sourceClassName, "javadoc", sourceFileName, lineNumber + errorLine))
    newStack.addAll(stack)
    throwable.setStackTrace(newStack as StackTraceElement[])
    throwable
  }
}
