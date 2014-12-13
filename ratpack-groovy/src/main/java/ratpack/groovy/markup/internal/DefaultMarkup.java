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

package ratpack.groovy.markup.internal;

import groovy.lang.Closure;
import groovy.xml.MarkupBuilder;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.groovy.markup.Markup;
import ratpack.handling.Context;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class DefaultMarkup implements Markup {

  private final CharSequence contentType;
  private final Charset encoding;
  private final Closure<?> definition;

  public DefaultMarkup(CharSequence contentType, Charset encoding, Closure<?> definition) {
    this.contentType = contentType;
    this.encoding = encoding;
    this.definition = definition;
  }

  @Override
  public CharSequence getContentType() {
    return contentType;
  }

  @Override
  public Charset getEncoding() {
    return encoding;
  }

  @Override
  public Closure<?> getDefinition() {
    return definition;
  }

  @Override
  public void render(Context context) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    OutputStreamWriter writer = new OutputStreamWriter(out, getEncoding());
    MarkupBuilder markupBuilder = new MarkupBuilder(writer);

    ClosureUtil.configureDelegateFirst(markupBuilder, markupBuilder, getDefinition());

    context.getResponse().contentType(getContentType()).send(out.toByteArray());
  }
}
