package com.bleedingwolf.ratpack

import groovy.text.SimpleTemplateEngine

import javax.servlet.http.HttpServletRequest

class TemplateRenderer {

  File dir

  TemplateRenderer(File dir) {
    this.dir = dir
  }

  String render(String templateName, context = [:]) {
    String text = ''

    try {
      text += loadTemplateText(templateName)
    } catch (java.io.IOException ex) {
      text += loadResource('com/bleedingwolf/ratpack/exception.html').text
      context = [
          title: 'Template Not Found',
          message: 'Template Not Found',
          metadata: [
              'Template Name': templateName,
          ],
          stacktrace: ""
      ]
    }

    renderTemplate(text, context)
  }

  String renderError(Map context) {
    String text = loadResource('com/bleedingwolf/ratpack/exception.html').text
    renderTemplate(text, context)
  }

  String renderException(Throwable ex, HttpServletRequest req) {
    def stackInfo = decodeStackTrace(ex)

    String text = loadResource('com/bleedingwolf/ratpack/exception.html').text
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

    renderTemplate(text, context)
  }

  protected loadTemplateText(String templateName) {
    new File(dir, templateName).text
  }

  protected Map decodeStackTrace(Throwable t) {
    // FIXME
    // this doesn't really make sense, but I'm not sure
    // how to create a `firstPartyPrefixes` list.
    def thirdPartyPrefixes = ['sun', 'java', 'groovy', 'org.codehaus', 'org.mortbay']

    String html = '';
    html += t.toString() + '\n'
    StackTraceElement rootCause = null

    for (StackTraceElement ste : t.getStackTrace()) {
      if (thirdPartyPrefixes.any { ste.className.startsWith(it) }) {
        html += "<span class='stack-thirdparty'>        at ${ste}\n</span>"
      } else {
        html += "        at ${ste}\n"
        if (null == rootCause) rootCause = ste
      }
    }

    return [html: html, rootCause: rootCause]
  }

  protected String renderTemplate(String text, Map context) {
    SimpleTemplateEngine engine = new SimpleTemplateEngine(new GroovyClassLoader())
    def template = engine.createTemplate(text).make(context)
    return template.toString()
  }

  protected InputStream loadResource(String path) {
    Thread.currentThread().contextClassLoader.getResourceAsStream(path)
  }
}
