package org.ratpackframework.http;

public interface HttpMethod {

  String getName();

  boolean isPost();

  boolean isGet();

  boolean isPut();

  boolean isDelete();

  boolean name(String name);

}
