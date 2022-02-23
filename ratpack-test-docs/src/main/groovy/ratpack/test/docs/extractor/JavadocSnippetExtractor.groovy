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

import org.apache.commons.text.StringEscapeUtils;
import ratpack.test.docs.SnippetExecuter;
import ratpack.test.docs.TestCodeSnippet;
import ratpack.test.docs.executer.ExceptionTransformer;

import java.util.regex.MatchResult
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavadocSnippetExtractor {

  private static Pattern JAVADOC_PATTERN = Pattern.compile(/(?ims)\/\*\*.*?\*\//);
  private static Pattern PACKAGE_PATTERN = Pattern.compile("package ([^ ;]+)");
  private static Pattern LINE_START_PATTERN = Pattern.compile("(?m)^");
  private static List<Map<String, String>> DELIMITER_TOKENS = new ArrayList<>();
  private static List<String> DETAGGED_TOKENS = Arrays.asList("\\{@literal (.+?)}", "\\{@code (.+?)}");

  static {
    Map<String, String> javadoc = new HashMap<>();
    javadoc.put("start", "{@code");
    javadoc.put("end", "}");

    Map<String, String> html = new HashMap<>();
    html.put("start", "<code>");
    html.put("end", "</code>");

    DELIMITER_TOKENS.add(javadoc);
    DELIMITER_TOKENS.add(html);
  }

  public static List<TestCodeSnippet> extract(File root, String include, String cssClass, SnippetExecuter executer) {
    List<TestCodeSnippet> snippets = new ArrayList<>();

    Pattern snippetTagPattern = Pattern.compile("(?ims)<([a-z]+)\\s+class\\s*=\\s*['\"]" + cssClass + "['\"]\\s*>.*?<\\s*/\\s*\\1>");
    List<String> filenames = new FileNameFinder().getFileNames(root.absolutePath, include);

    filenames.forEach { filename ->
      File file = new File(filename);
      addSnippets(snippets, file, snippetTagPattern, executer);
    }

    return snippets;
  }

  private static void addSnippets(List<TestCodeSnippet> snippets, File file, Pattern snippetTagPattern, SnippetExecuter executer) {
    String source = file.getText();
    String testName = calculateBaseName(file, source);
    Map<Integer, List<String>> snippetsByLine = findSnippetsByLine(source, snippetTagPattern);

    snippetsByLine.entrySet().forEach { entry ->
      entry.getValue().forEach { snippet ->
        snippets.add(createSnippet(testName, file, entry.getKey(), snippet, executer))
      }
    }
  }

  private static String calculateBaseName(File file, String source) {
    Matcher matcher = PACKAGE_PATTERN.matcher(source)
    matcher.find()
    MatchResult packageNameMatch = matcher.toMatchResult()
    String fileName = file.getName().substring(0, file.getName().lastIndexOf(".") - 1);
    return packageNameMatch.group(1) + "." + fileName;
  }

  private static List<String> findSnippetTags(String code, Pattern snippetTagPattern) {
    List<String> tags = new ArrayList<>();

    Matcher matcher = JAVADOC_PATTERN.matcher(code);
    matcher.results().forEach { match ->
      Matcher snippetMatcher = snippetTagPattern.matcher(match.group());
      snippetMatcher.results().forEach { snippetMatch ->
        tags.add(snippetMatch.group());
      }
    }
    return tags;
  }

  private static Map<Integer, List<String>> findSnippetsByLine(String source, Pattern snippetTagPattern) {
    List<String> snippetTags = findSnippetTags(source, snippetTagPattern);
    Map<Integer, List<String>> lineNumberToAssertions = new HashMap<>();

    int codeIndex = 0;
    for (String tag : snippetTags) {
      codeIndex = source.indexOf(tag, codeIndex);
      String subSource = source.substring(0, codeIndex);
      int lineNumber = LINE_START_PATTERN.matcher(subSource).results().count();
      codeIndex += tag.size();

      String assertion = extractSnippetFromTag(tag);

      lineNumberToAssertions.computeIfAbsent(lineNumber + 1) { i -> []}.add(assertion);
    }

    return lineNumberToAssertions;
  }

  private static String extractSnippetFromTag(String tag) {
    String tagInner = tag.substring(tag.indexOf(">") + 1, tag.lastIndexOf("<"));
    String html = tagInner.replaceAll("(?m)^\\s*\\*", "").trim();

//    String trimmed = [
//      [start: "{@code", end: "}"],
//      [start: "<code>", end: "</code>"]
//    ].inject(html) { h, delimiters ->
//      if (h.startsWith(delimiters.start) && h.endsWith(delimiters.end)) {
//        return h.subSequence(delimiters.start.length(), h.length() - delimiters.end.length());
//      }
//      return h;
//    }

    String trimmed = DELIMITER_TOKENS.stream().reduce(html, { h, delimiters ->
      if (h.startsWith(delimiters.get("start")) && h.endsWith(delimiters.get("end"))) {
        return h.subSequence(delimiters.start.length(), h.length() - delimiters.end.length());
      }
      return h;
    }, {a, b ->
      return b;
    })

//    String detagged = ["\\{@literal (.+?)}", "\\{@code (.+?)}"]
//      .inject(trimmed) { h, p ->
//        h.replaceAll(p, '$1');
//      }

    String detagged = DETAGGED_TOKENS.stream().reduce(trimmed, { h, p ->
      h.replaceAll(p, "\$1");
    });

    String snippet = StringEscapeUtils.unescapeHtml4(detagged);
    return snippet;
  }

  private static TestCodeSnippet createSnippet(String sourceClassName, File sourceFile, int lineNumber, String snippet, SnippetExecuter executer) {
    new TestCodeSnippet(snippet, sourceClassName, sourceClassName + ":" + lineNumber, executer, new ExceptionTransformer(sourceClassName, sourceFile.name, lineNumber));
  }

}
