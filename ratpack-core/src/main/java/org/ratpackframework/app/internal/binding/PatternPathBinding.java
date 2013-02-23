package org.ratpackframework.app.internal.binding;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternPathBinding implements PathBinding {

  final Pattern pattern;

  public PatternPathBinding(String pattern) {
    this.pattern = Pattern.compile(pattern);
  }

  @Override
  public Map<String, String> map(String path) {
    Matcher matcher = pattern.matcher(path);
    if (matcher.matches()) {
      Map<String, String> params = new LinkedHashMap<>();
      for (int i = 0; i < matcher.groupCount(); i++) {
        params.put("param" + i, matcher.group(i + 1));
      }
      return params;
    } else {
      return null;
    }
  }

}
