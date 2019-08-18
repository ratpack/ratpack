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

package ratpack.manual.snippets.extractor

import ratpack.test.internal.snippets.TestCodeSnippet
import ratpack.test.internal.snippets.executer.ExceptionTransformer
import ratpack.test.internal.snippets.executer.SnippetExecuter
import ratpack.util.RatpackVersion

import java.util.regex.Pattern

class ManualSnippetExtractor {

  static List<TestCodeSnippet> extract(File root, String cssClass, SnippetExecuter executer) {
    List<TestCodeSnippet> snippets = []

    def snippetBlockPattern = Pattern.compile(/(?ims)```$cssClass\n(.*?)\n```/)
    def filenames = new FileNameFinder().getFileNames(root.absolutePath, "*.md")

    filenames.each { filename ->
      def file = new File(filename)
      addSnippets(snippets, file, snippetBlockPattern, executer)
    }

    snippets
  }

  private static void addSnippets(List<TestCodeSnippet> snippets, File file, Pattern snippetBlockPattern, SnippetExecuter executer) {
    def source = file.text
    String testName = file.name
    Map<Integer, String> snippetsByLine = findSnippetsByLine(source, snippetBlockPattern)

    snippetsByLine.each { lineNumber, snippet ->
      snippets << createSnippet(testName, file, lineNumber, snippet, executer)
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
      def lineNumber = source.substring(0, codeIndex).readLines().size() + 2
      snippetBlocksByLine.put(lineNumber, performSubstitutions(extractSnippetFromBlock(block)))
      codeIndex += block.size()
    }

    snippetBlocksByLine
  }

  private static String extractSnippetFromBlock(String tag) {
    tag.substring(tag.indexOf("\n") + 1, tag.lastIndexOf("\n"))
  }

  /**
   * Perform the substitutions that {@code :ratpack-manual:tokeniseManual} would perform, as required by tested snippets.
   */
  private static String performSubstitutions(String snippet) {
    return snippet
      .replaceAll("@ratpack-version@", RatpackVersion.version)
      .replaceAll("@shadow-version@", "4.0.3")
  }

  private static TestCodeSnippet createSnippet(String sourceClassName, File sourceFile, int lineNumber, String snippet, SnippetExecuter executer) {
    new TestCodeSnippet(snippet, sourceClassName, sourceClassName + ":$lineNumber", executer, new ExceptionTransformer(sourceClassName, sourceFile.name, lineNumber))
  }

}
