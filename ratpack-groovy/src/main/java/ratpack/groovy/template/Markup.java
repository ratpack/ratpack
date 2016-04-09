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

package ratpack.groovy.template;

import groovy.lang.Closure;
import groovy.xml.MarkupBuilder;
import io.netty.util.CharsetUtil;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.handling.Context;
import ratpack.render.Renderable;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class Markup implements Renderable {

  private final CharSequence contentType;
  private final Charset encoding;
  private final Closure<?> definition;

  public Markup(CharSequence contentType, Charset encoding, Closure<?> definition) {
    this.contentType = contentType;
    this.encoding = encoding;
    this.definition = definition;
  }

  public CharSequence getContentType() {
    return contentType;
  }

  public Charset getEncoding() {
    return encoding;
  }

  public Closure<?> getDefinition() {
    return definition;
  }

  @Override
  public void render(Context context) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    OutputStreamWriter writer = new OutputStreamWriter(out, CharsetUtil.encoder(getEncoding()));
    MarkupBuilder markupBuilder = new MarkupBuilder(writer);

    ClosureUtil.configureDelegateFirst(markupBuilder, markupBuilder, getDefinition());

    context.getResponse().contentType(getContentType()).send(out.toByteArray());
  }
}
