package org.ratpackframework.path.internal;

import org.ratpackframework.path.PathBinding;
import org.ratpackframework.path.PathContext;
import org.ratpackframework.util.internal.Validations;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenPathBinding implements PathBinding {

  private final List<String> tokenNames;
  private final Pattern regex;
  private final boolean exact;

  public TokenPathBinding(String path, boolean exact) {
    this.exact = exact;
    Validations.noLeadingForwardSlash(path, "token path");

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
  public PathContext bind(String path, PathContext pathContext) {
    String regexString = regex.pattern();

    if (pathContext != null) {
      regexString = pathContext.join(regexString);
    }

    regexString = "(" + regexString + ")";
    if (!exact) {
      regexString = regexString.concat("(?:/.*)?");
    }

    Pattern bindPattern = Pattern.compile(regexString);

    Matcher matcher = bindPattern.matcher(path);
    if (matcher.matches()) {
      MatchResult matchResult = matcher.toMatchResult();
      String boundPath = matchResult.group(1);
      Map<String, String> params = new LinkedHashMap<>();
      int i = 2;
      for (String name : tokenNames) {
        params.put(name, matchResult.group(i++));
      }

      return new DefaultPathContext(path, boundPath, params, pathContext);
    } else {
      return null;
    }
  }
}
