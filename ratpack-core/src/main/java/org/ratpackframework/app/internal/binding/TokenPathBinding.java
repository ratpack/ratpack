package org.ratpackframework.app.internal.binding;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenPathBinding implements PathBinding {

  final List<String> tokenNames;
  final Pattern regex;

  public TokenPathBinding(String path) {
    List<String> names = new LinkedList<>();
    String regexString = Pattern.quote(path);

    Pattern placeholderPattern = Pattern.compile("(:\\w+)");
    Matcher matchResult = placeholderPattern.matcher(path);
    while (matchResult.find()) {
      String name = matchResult.group();
      regexString = regexString.replaceFirst(name, "\\\\E([^/?&#]+)\\\\Q");
      names.add(name.substring(1));
    }

    this.regex = Pattern.compile(regexString);
    this.tokenNames = Collections.unmodifiableList(names);
  }

  @Override
  public Map<String, String> map(String path) {
    Matcher matcher = regex.matcher(path);
    if (matcher.matches()) {
      MatchResult matchResult = matcher.toMatchResult();
      Map<String, String> params = new LinkedHashMap<>();
      int i = 1;
      for (String name : tokenNames) {
        params.put(name, matchResult.group(i++));
      }
      return params;
    } else {
      return null;
    }
  }

}
