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

import java.io.File;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavadocSnippetExtractor extends AbstractSnippetExtractor {

  private static final Pattern JAVADOC_PATTERN = Pattern.compile("(?ims)/\\*\\*.*?\\*/");
  private static final Pattern PACKAGE_PATTERN = Pattern.compile("package ([^ ;]+)");
  private static final Pattern HTML_PATTERN = Pattern.compile("(?m)^\\s*\\*");
  private static final List<Map<String, String>> DELIMITER_TOKENS = new ArrayList<>();
  private static final List<String> DETAGGED_TOKENS = Arrays.asList("\\{@literal (.+?)}", "\\{@code (.+?)}");

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

  @Override
  protected Pattern getExtractPattern(String cssClass) {
    return Pattern.compile("(?ims)<([a-z]+)\\s+class\\s*=\\s*['\"]" + cssClass + "['\"]\\s*>.*?<\\s*/\\s*\\1>");
  }

  @Override
  protected String generateTestName(File file, String source) {
    Matcher matcher = PACKAGE_PATTERN.matcher(source);
    matcher.find();
    MatchResult packageNameMatch = matcher.toMatchResult();
    String fileName = file.getName().substring(0, file.getName().lastIndexOf(".") - 1);
    return packageNameMatch.group(1) + "." + fileName;
  }

  @Override
  protected List<String> findSnippetBlocks(String code, Pattern snippetTagPattern) {
    List<String> tags = new ArrayList<>();

    Matcher matcher = JAVADOC_PATTERN.matcher(code);
    while (matcher.find()) {
      MatchResult match = matcher.toMatchResult();
      Matcher snippetMatcher = snippetTagPattern.matcher(match.group());
      while (snippetMatcher.find()) {
        MatchResult snippetMatch = snippetMatcher.toMatchResult();
        tags.add(snippetMatch.group());
      }
    }
    return tags;
  }

  @Override
  protected String extractSnippetFromBlock(String block) {
    String tagInner = block.substring(block.indexOf(">") + 1, block.lastIndexOf("<"));
    String html = HTML_PATTERN.matcher(tagInner).replaceAll("").trim();

    String trimmed = DELIMITER_TOKENS.stream().reduce(
      html,
      (h, delimiters) -> {
        if (h.startsWith(delimiters.get("start")) && h.endsWith(delimiters.get("end"))) {
          return h.subSequence(delimiters.get("start").length(), h.length() - delimiters.get("end").length()).toString();
        }
        return h;
      },
      (a, b) -> b
    );

    String detagged = DETAGGED_TOKENS.stream().reduce(trimmed, (h, p) ->
      h.replaceAll(p, "$1")
    );

    return StringEscapeUtils.unescapeHtml4(detagged);
  }

}
