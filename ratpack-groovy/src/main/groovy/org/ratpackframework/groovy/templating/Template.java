package org.ratpackframework.groovy.templating;

import org.ratpackframework.groovy.templating.TemplateModel;

import java.util.Map;

public interface Template {

  TemplateModel getModel();

  String render(String templateName) throws Exception;

  String render(Map<String, ?> model, String templateName) throws Exception;

}
