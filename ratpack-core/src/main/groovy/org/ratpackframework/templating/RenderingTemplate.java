/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ratpackframework.templating;

import groovy.lang.Writable;
import org.vertx.java.core.Handler;

import java.util.LinkedList;
import java.util.List;

public class RenderingTemplate implements Handler<Writable> {

  private final TemplateId templateId;
  private Writable writable;
  private String content;

  private final List<RenderingTemplate> children = new LinkedList<RenderingTemplate>();

  public RenderingTemplate(TemplateId templateId) {
    this.templateId = templateId;
  }

  public List<RenderingTemplate> getChildren() {
    return children;
  }

  public TemplateId getId() {
    return templateId;
  }

  @Override
  public void handle(Writable writable) {
    this.writable = writable;
  }

  public void complete() {
    if (content != null) {
      return;
    }

    for (RenderingTemplate child : children) {
      child.complete();
    }

    content = writable == null ? "<exception>" : writable.toString();
  }

  @Override
  public String toString() {
    if (writable == null) {
      return "<rendering>";
    } else if (content == null) {
      return "<pending>";
    } else {
      return content;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RenderingTemplate that = (RenderingTemplate) o;

    if (!templateId.equals(that.templateId)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return templateId.hashCode();
  }
}
