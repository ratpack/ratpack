package org.ratpackframework.http.internal;

import org.ratpackframework.http.HttpMethod;

public class DefaultHttpMethod implements HttpMethod {

  private final String name;

  public DefaultHttpMethod(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isPost() {
    return name.equals("POST");
  }

  @Override
  public boolean isGet() {
    return name.equals("GET");
  }

  @Override
  public boolean isPut() {
    return name.equals("PUT");
  }

  @Override
  public boolean isDelete() {
    return name.equals("DELETE");
  }

  @Override
  public boolean name(String name) {
    return this.name.equals(name.toUpperCase());
  }

}
