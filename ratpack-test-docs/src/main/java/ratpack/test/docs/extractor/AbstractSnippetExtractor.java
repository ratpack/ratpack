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

package ratpack.test.docs.extractor;

import groovy.util.FileNameFinder;
import org.apache.tools.ant.util.FileUtils;
import ratpack.func.Exceptions;
import ratpack.test.docs.SnippetExecuter;
import ratpack.test.docs.SnippetExtractor;
import ratpack.test.docs.TestCodeSnippet;
import ratpack.test.docs.executer.ExceptionTransformer;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractSnippetExtractor implements SnippetExtractor {

  private static final Pattern LINE_START_PATTERN = Pattern.compile("(?m)^");

  protected abstract Pattern getExtractPattern(String cssClass);
  protected abstract String generateTestName(File file, String source);
  protected abstract String extractSnippetFromBlock(String block);

  public List<TestCodeSnippet> extract(File root, String include, String cssClass, SnippetExecuter executer) {
    List<TestCodeSnippet> snippets = new ArrayList<>();

    Pattern snippetTagPattern = getExtractPattern(cssClass);
    List<String> filenames = new FileNameFinder().getFileNames(root.getAbsolutePath(), include);

    filenames.forEach(filename -> {
      File file = new File(filename);
      addSnippets(snippets, file, snippetTagPattern, executer);
    });

    return snippets;
  }

  protected void addSnippets(List<TestCodeSnippet> snippets, File file, Pattern snippetTagPattern, SnippetExecuter executer) {
    String source = Exceptions.uncheck(() -> FileUtils.readFully(new FileReader(file)));
    String testName = generateTestName(file, source);
    Map<Integer, List<String>> snippetsByLine = findSnippetsByLine(source, snippetTagPattern);

    snippetsByLine.forEach((lineNumber, snippetBlocks) ->
      snippetBlocks.forEach(snippet ->
        snippets.add(createSnippet(testName, file, lineNumber, snippet, executer))
      )
    );
  }

  protected Map<Integer, List<String>> findSnippetsByLine(String source, Pattern snippetTagPattern) {
    List<String> snippetBlocks = findSnippetBlocks(source, snippetTagPattern);
    Map<Integer, List<String>> lineNumberToAssertions = new HashMap<>();

    int codeIndex = 0;
    for (String block : snippetBlocks) {
      codeIndex = source.indexOf(block, codeIndex);
      String subSource = source.substring(0, codeIndex);
      int lineNumber = 0;
      Matcher m = LINE_START_PATTERN.matcher(subSource);
      while (m.find()) {
        lineNumber++;
      }
      codeIndex += block.length();
      String assertion = performSubstitutions(extractSnippetFromBlock(block));
      lineNumberToAssertions.computeIfAbsent(lineNumber + 1,  i -> new ArrayList<>()).add(assertion);
    }

    return lineNumberToAssertions;
  }

  protected List<String> findSnippetBlocks(String code, Pattern snippetTagPattern) {
    List<String> blocks = new ArrayList<>();
    Matcher matcher = snippetTagPattern.matcher(code);
    while (matcher.find()) {
      MatchResult match = matcher.toMatchResult();
      blocks.add(match.group());
    }
    return blocks;
  }

  protected String performSubstitutions(String snippet) {
    return snippet;
  }

  protected TestCodeSnippet createSnippet(String sourceClassName, File sourceFile, int lineNumber, String snippet, SnippetExecuter executer) {
    return new TestCodeSnippet(snippet, sourceClassName, sourceClassName + ":" + lineNumber, executer, new ExceptionTransformer(sourceClassName, sourceFile.getName(), lineNumber));
  }
}
