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

package ratpack.test.internal.snippets.executer

class ExceptionTransformer {

  final String sourceClassName
  final String sourceFileName
  final Integer lineNumber

  ExceptionTransformer(String sourceClassName, String sourceFileName, Integer lineNumber) {
    this.sourceClassName = sourceClassName
    this.sourceFileName = sourceFileName
    this.lineNumber = lineNumber
  }

  Throwable transform(Throwable throwable, Integer offset) throws Exception {
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
    errorLine = errorLine - offset
    StackTraceElement[] stack = throwable.getStackTrace()
    List<StackTraceElement> newStack = new ArrayList<StackTraceElement>(stack.length + 1)
    newStack.add(new StackTraceElement(sourceClassName, "javadoc", sourceFileName, lineNumber + errorLine))
    newStack.addAll(stack)
    throwable.setStackTrace(newStack as StackTraceElement[])
    throwable
  }
}
