package org.ratpackframework.templating.internal;

import groovy.lang.Binding;
import groovy.lang.Script;
import org.ratpackframework.templating.Template;
import org.ratpackframework.templating.TemplateModel;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class TemplateScript extends Script implements Template {

  private TemplateModel model;
  private List<Object> parts;
  private NestedRenderer renderer;

  @SuppressWarnings("UnusedDeclaration")
  protected TemplateScript() {
  }

  @SuppressWarnings("UnusedDeclaration")
  protected TemplateScript(Binding binding) {
    super(binding);
  }

  @SuppressWarnings("UnusedDeclaration")
  public List<Object> getParts() {
    return parts;
  }

  public void setParts(List<Object> parts) {
    this.parts = parts;
  }

  @Override
  public TemplateModel getModel() {
    return model;
  }

  public void setModel(TemplateModel model) {
    this.model = model;
  }

  public void setRenderer(NestedRenderer renderer) {
    this.renderer = renderer;
  }

  @Override
  public void render(String templateName) {
    parts.add(renderer.render(templateName, Collections.<String, Object>emptyMap()));
  }

  @Override
  public void render(Map<String, ?> model, String templateName) {
    parts.add(renderer.render(templateName, model));
  }

}
