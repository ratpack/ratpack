package org.ratpackframework.templating;

import java.util.Map;

public interface Template {

  TemplateModel getModel();

  void render(String templateName);

  void render(Map<String, ?> model, String templateName);

}
