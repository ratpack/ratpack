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

package ratpack.manual.snippets

import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.control.messages.SyntaxErrorMessage
import ratpack.manual.snippets.fixtures.SnippetFixture
import ratpack.func.Transformer

import java.util.regex.Pattern

class ManualSnippetExtractor {

  static List<TestCodeSnippet> extract(File root, String cssClass, SnippetFixture fixture) {
    List<TestCodeSnippet> snippets = []

    def snippetBlockPattern = Pattern.compile(/(?ims)```$cssClass\n(.*?)\n```/)
    def filenames = new FileNameFinder().getFileNames(root.absolutePath, "*.md")

    filenames.each { filename ->
      def file = new File(filename)
      addSnippets(snippets, file, snippetBlockPattern, fixture)
    }

    snippets
  }

  private static void addSnippets(List<TestCodeSnippet> snippets, File file, Pattern snippetBlockPattern, SnippetFixture snippetFixture) {
    def source = file.text
    String testName = file.name
    Map<Integer, String> snippetsByLine = findSnippetsByLine(source, snippetBlockPattern)

    snippetsByLine.each { lineNumber, snippet ->
      snippets << createSnippet(testName, file, lineNumber, snippet, snippetFixture)
    }
  }

  private static List<String> findSnippetBlocks(String code, Pattern snippetTagPattern) {
    List<String> tags = []
    code.eachMatch(snippetTagPattern) { matches ->
      tags.add(matches[0])
    }
    tags
  }

  private static Map<Integer, String> findSnippetsByLine(String source, Pattern snippetTagPattern) {
    List<String> snippetBlocks = findSnippetBlocks(source, snippetTagPattern)
    Map snippetBlocksByLine = [:]

    int codeIndex = 0
    snippetBlocks.each { block ->
      codeIndex = source.indexOf(block, codeIndex)
      def lineNumber = source.substring(0, codeIndex).readLines().size() + 1
      snippetBlocksByLine.put(lineNumber, extractSnippetFromBlock(block))
      codeIndex += block.size()
    }

    snippetBlocksByLine
  }

  private static String extractSnippetFromBlock(String tag) {
    tag.substring(tag.indexOf("\n") + 1, tag.lastIndexOf("\n"))
  }

  private static TestCodeSnippet createSnippet(String sourceClassName, File sourceFile, int lineNumber, String snippet, SnippetFixture fixture) {
    new TestCodeSnippet(snippet, sourceClassName, sourceClassName + ":$lineNumber", fixture, new Transformer<Throwable, Throwable>() {
      @Override
      Throwable transform(Throwable t) {
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
        errorLine = errorLine - fixture.pre().split("\n").size()
        StackTraceElement[] stack = t.getStackTrace()
        List<StackTraceElement> newStack = new ArrayList<StackTraceElement>(stack.length + 1)
        newStack.add(new StackTraceElement(sourceClassName, "javadoc", sourceFile.name, lineNumber + errorLine))
        newStack.addAll(stack)
        t.setStackTrace(newStack as StackTraceElement[])

        t
      }
    })

  }

}