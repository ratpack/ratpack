package org.ratpackframework.path.internal;

import org.ratpackframework.path.PathContext;
import org.ratpackframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class DefaultPathContext implements PathContext {

  private final String binding;
  private final String bindingTerminated;
  private final String pastBinding;

  private final Map<String, String> tokens;
  private final Map<String, String> allTokens;
  private final PathContext parent;

  public DefaultPathContext(String path, String binding, Map<String, String> tokens, PathContext parent) {
    this.binding = binding;
    this.tokens = tokens;
    this.parent = parent;

    if (parent == null) {
      allTokens = tokens;
    } else {
      allTokens = new LinkedHashMap<>(parent.getAllTokens());
      allTokens.putAll(tokens);
    }

    this.bindingTerminated = binding + "/";

    if (path.equals(binding)) {
      pastBinding = "";
    } else if (path.startsWith(bindingTerminated)) {
      pastBinding = path.substring(bindingTerminated.length());
    } else {
      throw new IllegalArgumentException(String.format("Path '%s' is not a child of '%s'", path, binding));
    }
  }

  @Override
  public String getPastBinding() {
    return pastBinding;
  }

  @Override
  public String getBoundTo() {
    return binding;
  }

  @Override
  public String join(String path) {
    return CollectionUtils.join("/", this.binding, path);
  }

  @Override
  public Map<String, String> getTokens() {
    return tokens;
  }

  @Override
  public Map<String, String> getAllTokens() {
    return allTokens;
  }
}
