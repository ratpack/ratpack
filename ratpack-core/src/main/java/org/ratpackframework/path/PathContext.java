package org.ratpackframework.path;

import java.util.Map;

public interface PathContext {

  String getBoundTo();

  String getPastBinding();

  String join(String path);

  Map<String, String> getTokens();

  Map<String, String> getAllTokens();
}
