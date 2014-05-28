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

package ratpack.groovy.templating.internal;

import com.google.common.escape.Escaper;
import com.google.common.html.HtmlEscapers;
import groovy.lang.Script;
import io.netty.buffer.ByteBuf;
import ratpack.groovy.templating.TemplateModel;
import ratpack.groovy.templating.TemplateScript;
import ratpack.util.internal.IoUtils;

import java.util.Collections;
import java.util.Map;

public abstract class DefaultTemplateScript extends Script implements TemplateScript {

  private static final Escaper ESCAPER = HtmlEscapers.htmlEscaper();

  private final TemplateModel model;
  private final NestedRenderer renderer;
  private final ByteBuf buffer;

  protected DefaultTemplateScript(TemplateModel model, ByteBuf buffer, NestedRenderer renderer) {
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

  @Override
  public String html(Object value) {
    return ESCAPER.escape(value.toString());
  }

  //CHECKSTYLE:OFF
  @SuppressWarnings("UnusedDeclaration")
  public void $(CharSequence charSequence) {
    //CHECKSTYLE:ON
    buffer.writeBytes(IoUtils.utf8Bytes(charSequence.toString()));
  }

}
