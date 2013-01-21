package com.bleedingwolf.ratpack

import groovy.text.SimpleTemplateEngine
import groovy.transform.CompileStatic

import javax.servlet.http.HttpServletRequest

@CompileStatic
class TemplateRenderer {

  File dir

  TemplateRenderer(File dir) {
    this.dir = dir
  }

  Writable render(String templateName, Map<String, Object> context = [:]) {
    innerRender(new GroovyClassLoader(), templateName, context)
  }

  Writable render(Reader template, Map<String, Object> context = [:]) {
    innerRender(new GroovyClassLoader(), template, context)
  }

  protected Writable innerRender(ClassLoader classLoader, String templateName, Map<String, Object> context) {
    Reader reader
    try {
      reader = loadTemplate(templateName)
    } catch (java.io.IOException ignore) {
      reader = loadResource('com/bleedingwolf/ratpack/exception.html').newReader()
      context = [
          title: 'Template Not Found',
          message: 'Template Not Found',
          metadata: [
              'Template Name': templateName,
          ],
          stacktrace: ""
      ] as Map<String, Object>
    }

    innerRender(classLoader, reader, context)
  }

  protected Writable innerRender(ClassLoader classLoader, Reader template, Map<String, Object> context) {
    SimpleTemplateEngine engine = new SimpleTemplateEngine(classLoader)
    if (context.containsKey("render")) {
      throw new IllegalStateException("The context for render operations cannot contain an item called 'render'")
    }
    context.render = { Map innerContext = [:], String innerTemplate ->
      innerRender(classLoader, innerTemplate, innerContext)
    }

    engine.createTemplate(template).make(context)
  }

  @SuppressWarnings("GrMethodMayBeStatic")
  String renderError(Map context) {
    render(loadResource('com/bleedingwolf/ratpack/exception.html').newReader(), context)
  }

  @SuppressWarnings("GrMethodMayBeStatic")
  String renderException(Throwable ex, HttpServletRequest req) {
    def stackInfo = decodeStackTrace(ex)

    def reader = loadResource('com/bleedingwolf/ratpack/exception.html').newReader()
    Map context = [
        title: ex.class.name,
        message: ex.message,
        metadata: [
            'Request Method': req.method.toUpperCase(),
            'Request URL': req.requestURL,
            'Exception Type': ex.class.name,
            'Exception Location': "${stackInfo.rootCause.fileName}, line ${stackInfo.rootCause.lineNumber}",
        ],
        stacktrace: stackInfo.html
    ]

    render(reader, context)
  }

  protected Reader loadTemplate(String templateName) {
    new File(dir, templateName).newReader()
  }

  private static class DecodedStackTrace {
    final String html
    final StackTraceElement rootCause

    DecodedStackTrace(String html, StackTraceElement rootCause) {
      this.html = html
      this.rootCause = rootCause
    }
  }

  protected static DecodedStackTrace decodeStackTrace(Throwable t) {
    // FIXME
    // this doesn't really make sense, but I'm not sure
    // how to create a `firstPartyPrefixes` list.
    def thirdPartyPrefixes = ['sun', 'java', 'groovy', 'org.codehaus', 'org.mortbay']

    String html = '';
    html += t.toString() + '\n'
    StackTraceElement rootCause = null

    for (StackTraceElement ste : t.getStackTrace()) {
      if (thirdPartyPrefixes.any { String it -> ste.className.startsWith(it) }) {
        html += "<span class='stack-thirdparty'>        at ${ste}\n</span>"
      } else {
        html += "        at ${ste}\n"
        if (null == rootCause) rootCause = ste
      }
    }

    return new DecodedStackTrace(html, rootCause)
  }

  protected static InputStream loadResource(String path) {
    Thread.currentThread().contextClassLoader.getResourceAsStream(path)
  }
}
