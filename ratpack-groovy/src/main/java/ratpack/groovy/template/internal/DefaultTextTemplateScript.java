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

package ratpack.groovy.template.internal;

import com.google.common.escape.Escaper;
import com.google.common.html.HtmlEscapers;
import com.google.common.net.UrlEscapers;
import groovy.lang.Script;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.CharsetUtil;
import ratpack.groovy.template.TextTemplateModel;
import ratpack.groovy.template.TextTemplateScript;

import java.nio.CharBuffer;
import java.util.Collections;
import java.util.Map;

public abstract class DefaultTextTemplateScript extends Script implements TextTemplateScript {

  private static final Escaper HTML_ESCAPER = HtmlEscapers.htmlEscaper();
  private static final Escaper URL_PATH_SEGMENT_ESCAPER = UrlEscapers.urlPathSegmentEscaper();
  private static final Escaper URL_PARAM_ESCAPER = UrlEscapers.urlFormParameterEscaper();

  private final TextTemplateModel model;
  private final NestedRenderer renderer;
  private final ByteBuf buffer;

  protected DefaultTextTemplateScript(TextTemplateModel model, ByteBuf buffer, NestedRenderer renderer) {
    this.model = model;
    this.buffer = buffer;
    this.renderer = renderer;
  }

  public TextTemplateModel getModel() {
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
    return value == null ? "" : HTML_ESCAPER.escape(value.toString());
  }

  @Override
  public String urlPathSegment(Object value) {
    return value == null ? "" : URL_PATH_SEGMENT_ESCAPER.escape(value.toString());
  }

  @Override
  public String urlParam(Object value) {
    return value == null ? "" : URL_PARAM_ESCAPER.escape(value.toString());
  }

  //CHECKSTYLE:OFF
  @SuppressWarnings("UnusedDeclaration")
  public void $(CharSequence charSequence) {
    //CHECKSTYLE:ON
    ByteBuf byteBuf = ByteBufUtil.encodeString(buffer.alloc(), CharBuffer.wrap(charSequence.toString()), CharsetUtil.UTF_8);
    buffer.writeBytes(byteBuf);
    byteBuf.release();
  }

}
