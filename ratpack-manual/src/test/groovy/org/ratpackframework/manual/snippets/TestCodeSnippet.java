package org.ratpackframework.manual.snippets;

import org.ratpackframework.manual.snippets.fixtures.SnippetFixture;
import org.ratpackframework.util.Transformer;

public class TestCodeSnippet {

  private final String snippet;
  private final String className;
  private final String testName;
  private final SnippetFixture fixture;
  private final Transformer<Throwable, Throwable> exceptionTransformer;

  public TestCodeSnippet(String snippet, String className, String testName, SnippetFixture fixture, Transformer<Throwable, Throwable> exceptionTransformer) {
    this.snippet = snippet;
    this.className = className;
    this.testName = testName;
    this.fixture = fixture;
    this.exceptionTransformer = exceptionTransformer;
  }

  public String getSnippet() {
    return snippet;
  }

  public String getClassName() {
    return className;
  }

  public String getTestName() {
    return testName;
  }

  public SnippetFixture getFixture() {
    return fixture;
  }

  public Transformer<Throwable, Throwable> getExceptionTransformer() {
    return exceptionTransformer;
  }

}
