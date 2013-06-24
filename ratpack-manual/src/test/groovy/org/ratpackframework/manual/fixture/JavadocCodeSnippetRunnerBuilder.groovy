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

package org.ratpackframework.manual.fixture

import org.apache.commons.lang3.StringEscapeUtils
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.control.messages.SyntaxErrorMessage
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier

import java.util.regex.MatchResult
import java.util.regex.Pattern

class JavadocCodeSnippetRunnerBuilder {

  private static Pattern javadocPattern =
    Pattern.compile(/(?ims)\/\*\*.*?\*\//)

  static List<Runner> build(Class<?> clazz, File root, String include, String cssClass, ScriptRunner scriptRunner) {
    List<Runner> runners = []
    def snippetTagPattern = Pattern.compile(/(?ims)<([a-z]+)\s+class\s*=\s*['"]$cssClass['"]\s*>.*?<\s*\/\s*\1>/)
    def filenames = new FileNameFinder().getFileNames(root.absolutePath, include)
    filenames.each { filename ->
      def file = new File(filename)
      addTests(clazz, runners, file, snippetTagPattern, scriptRunner)
    }

    runners
  }

  private static void addTests(Class<?> clazz, List<Runner> runners, File file, Pattern snippetTagPattern, ScriptRunner scriptRunner) {
    def source = file.text
    String testName = calculateBaseName(file, source)
    Map<Integer, String> snippetsByLine = findSnippetsByLine(source, snippetTagPattern)
    snippetsByLine.each { lineNumber, snippets ->
      snippets.each { snippet ->
        runners << createRunner(clazz, testName, file, lineNumber, snippet, scriptRunner)
      }
    }
  }

  private static String calculateBaseName(File file, String source) {
    MatchResult packageNameMatch = source =~ "package ([^ ;]+)"
    packageNameMatch[0][1] + "." + (file.name[0..file.name.lastIndexOf(".") - 1])
  }

  private static List<String> findSnippetTags(String code, Pattern snippetTagPattern) {
    List<String> tags = []

    code.eachMatch(javadocPattern) { String javadoc ->
      tags.addAll(javadoc.findAll(snippetTagPattern))
    }

    tags
  }

  private static Map<Integer, String> findSnippetsByLine(String source, Pattern snippetTagPattern) {
    List<String> snippetTags = findSnippetTags(source, snippetTagPattern)
    Map lineNumberToAssertions = [:]

    int codeIndex = 0
    snippetTags.each { tag ->
      codeIndex = source.indexOf(tag, codeIndex)
      int lineNumber = source.substring(0, codeIndex).findAll("(?m)^").size()
      codeIndex += tag.size()

      String assertion = extractSnippetFromTag(tag)

      lineNumberToAssertions.get(lineNumber, []) << assertion
    }

    lineNumberToAssertions
  }

  private static Runner createRunner(Class<?> testClass, String sourceClassName, File sourceFile, int lineNumber, String snippet, ScriptRunner scriptRunner) {
    def description = Description.createTestDescription(testClass, sourceClassName + ":$lineNumber")
    new Runner() {
      @Override
      Description getDescription() {
        description
      }

      @Override
      void run(RunNotifier notifier) {
        try {
          notifier.fireTestStarted(getDescription())
          scriptRunner.runScript(snippet, sourceClassName)
        } catch (Throwable t) {
          def errorLine = 0

          if (t instanceof MultipleCompilationErrorsException) {
            def compilationException = t as MultipleCompilationErrorsException
            def error = compilationException.errorCollector.getError(0)
            if (error instanceof SyntaxErrorMessage) {
              errorLine = error.cause.line
            }
          } else {
            def frame = t.getStackTrace().find { it.fileName == sourceClassName }
            if (frame) {
              errorLine = frame.lineNumber
            }
          }
          errorLine = errorLine - scriptRunner.scriptLineOffset
          StackTraceElement[] stack = t.getStackTrace()
          List<StackTraceElement> newStack = new ArrayList<StackTraceElement>(stack.length + 1)
          newStack.add(new StackTraceElement(sourceClassName, "javadoc", sourceFile.name, lineNumber + errorLine))
          newStack.addAll(stack)
          t.setStackTrace(newStack as StackTraceElement[])
          notifier.fireTestFailure(new Failure(getDescription(), t))
        } finally {
          notifier.fireTestFinished(getDescription())
        }
      }

      @Override
      int testCount() {
        1
      }
    }
  }

  private static String extractSnippetFromTag(String tag) {
    String tagInner = tag.substring(tag.indexOf(">") + 1, tag.lastIndexOf("<"))
    String html = tagInner.replaceAll("(?m)^\\s*\\*", "")
    String deliteral = html.replaceAll("\\{@literal (.+?)}", '$1')
    def snippet = StringEscapeUtils.unescapeHtml4(deliteral)
    snippet
  }

}
