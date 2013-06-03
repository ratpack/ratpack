/*
 * Copyright 2013 the original author or authors.
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

package org.ratpackframework.groovy.templating.internal;

import groovy.lang.Script;
import io.netty.buffer.ByteBuf;
import org.ratpackframework.groovy.templating.Template;
import org.ratpackframework.groovy.templating.TemplateModel;
import org.ratpackframework.util.internal.IoUtils;

import java.util.Collections;
import java.util.Map;

public abstract class TemplateScript extends Script implements Template {

  private final TemplateModel model;
  private final NestedRenderer renderer;
  private final ByteBuf buffer;

  protected TemplateScript(TemplateModel model, ByteBuf buffer, NestedRenderer renderer) {
    this.model = model;
    this.buffer = buffer;
    this.renderer = renderer;
  }

  public TemplateModel getModel() {
    return model;
  }

  public String render(String templateName) throws Exception {
    return render(Collections.<String, Object>emptyMap(), templateName);
  }

  public String render(Map<String, ?> model, String templateName) throws Exception {
    renderer.render(templateName, model);
    return "";
  }

  //CHECKSTYLE:OFF
  public void $(CharSequence charSequence) {
  //CHECKSTYLE:ON
    buffer.writeBytes(IoUtils.utf8Bytes(charSequence.toString()));
  }

}
