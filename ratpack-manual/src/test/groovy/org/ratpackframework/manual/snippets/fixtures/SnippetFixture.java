package org.ratpackframework.manual.snippets.fixtures;

public interface SnippetFixture {

  void setup();

  void cleanup();

  String pre();

  String post();

}
