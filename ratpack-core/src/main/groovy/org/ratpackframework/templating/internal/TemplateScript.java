package org.ratpackframework.templating.internal;

import groovy.lang.Script;
import org.ratpackframework.templating.Template;
import org.ratpackframework.templating.TemplateModel;

import java.lang.ref.Reference;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class TemplateScript extends Script implements Template {

  private final String templateName;
  private final TemplateModel model;
  private final List<Object> parts;
  private final NestedRenderer renderer;

  private boolean outputMode = false;

  protected TemplateScript(String templateName, TemplateModel model, List<Object> parts, NestedRenderer renderer) {
    this.templateName = templateName;
    this.model = model;
    this.parts = parts;
    this.renderer = renderer;
  }

  @Override
  public TemplateModel getModel() {
    return model;
  }

  public void $o() { // enter output mode
    this.outputMode = true;
  }

  public void $c() { // enter code mode
    this.outputMode = false;
  }

  @Override
  public void render(String templateName) {
    if (outputMode) {
      invalidRender();
    }
    parts.add(renderer.render(templateName, Collections.<String, Object>emptyMap()));
  }

  private void invalidRender() {
    throw new InvalidTemplateException(templateName, "The render() method can only be called from <% %> blocks (not ${} or <%= %> blocks)");
  }

  @Override
  public void render(Map<String, ?> model, String templateName) {
    if (outputMode) {
      invalidRender();
    }
    parts.add(renderer.render(templateName, model));
  }

  public void str(CharSequence charSequence) {
    parts.add(charSequence);
  }

}
