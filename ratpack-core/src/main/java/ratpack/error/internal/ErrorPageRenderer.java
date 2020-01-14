/*
 * Copyright 2014 the original author or authors.
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

package ratpack.error.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.escape.Escaper;
import com.google.common.html.HtmlEscapers;
import com.google.common.io.CharStreams;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.util.CharsetUtil;
import ratpack.handling.Context;
import ratpack.http.internal.HttpHeaderConstants;

import java.io.*;
import java.util.Map;
import java.util.function.Consumer;

public abstract class ErrorPageRenderer {

  private static final Escaper HTML_ESCAPER = HtmlEscapers.htmlEscaper();

  private String style;

  public ErrorPageRenderer() {
    if (style == null) {
      InputStream resourceAsStream = ErrorPageRenderer.class.getResourceAsStream("error-template-style.css");
      if (resourceAsStream == null) {
        throw new IllegalStateException("Couldn't find style resource");
      }

      InputStreamReader reader = new InputStreamReader(resourceAsStream, CharsetUtil.decoder(CharsetUtil.UTF_8));
      try {
        style = CharStreams.toString(reader);
      } catch (IOException e) {
        throw new IllegalStateException("Could not read style stream", e);
      }
    }

    render();
  }

  protected abstract void render();

  protected void stack(BodyWriter w, String heading, Throwable throwable) {
    if (heading != null) {
      w.println("<div id=\"stack-header\">")
        .println("<div class=\"wrapper\">")
        .print("<h3>").escape(heading).print("</h3>")
        .println("</div>")
        .println("</div>");
    }
    w.println("<div class=\"wrapper\">")
      .println("<div class=\"stack\">");
    w.print("<pre><code>");
    throwable(w, throwable, false);

    w.println("</pre></code>")
      .println("</div>")
      .println("</div>");
  }

  protected void throwable(BodyWriter w, Throwable throwable, boolean isCause) {
    if (throwable != null) {
      if (isCause) {
        w.escape("Caused by: ");
      }

      w.escapeln(throwable.toString());
      for (StackTraceElement ste : throwable.getStackTrace()) {
        String className = ste.getClassName();
        if (className.startsWith("ratpack")
          || className.startsWith("io.netty")
          || className.startsWith("com.google")
          || className.startsWith("java")
          || className.startsWith("org.springsource.loaded")
          ) {
          w.print("<span class='stack-core'>  at ").escape(ste.toString()).println("</span>");
        } else {
          w.print("  at ").escape(ste.toString()).println("");
        }
      }

      throwable(w, throwable.getCause(), true);
    }
  }

  protected static class BodyWriter {
    private final PrintWriter writer;

    private BodyWriter(PrintWriter writer) {
      this.writer = writer;
    }

    BodyWriter print(String string) {
      writer.print(string);
      return this;
    }

    BodyWriter println(String string) {
      writer.println(string);
      return this;
    }

    BodyWriter escape(String string) {
      return print(HTML_ESCAPER.escape(string));
    }

    BodyWriter escapeln(String string) {
      return println(HTML_ESCAPER.escape(string));
    }
  }

  protected void messages(BodyWriter writer, String heading, Runnable block) {
    writer
      .println("<div class=\"wrapper\">")
      .println("<div id=\"messages\">")
      .println("<section>")
      .print("<h2>").escape(heading).println("</h2>");

    block.run();
    writer
      .println("<p><em>Note: This page will only be visible during development.</em></p>")
      .println("</section>")
      .println("</div>")
      .println("</div>");
  }

  protected void meta(BodyWriter w, Consumer<ImmutableMap.Builder<String, Object>> meta) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    meta.accept(builder);

    w.println("<table class=\"meta\">");
    for (Map.Entry<String, Object> entry : builder.build().entrySet()) {
      w.print("<tr><th>").escape(entry.getKey()).print("</th><td>").escape(entry.getValue().toString()).println("</td></tr>");
    }
    w.println("</table>");
  }

  protected void render(Context context, String pageTitle, Consumer<? super BodyWriter> body) {
    ByteBuf buffer = context.get(ByteBufAllocator.class).buffer();
    OutputStream out = new ByteBufOutputStream(buffer);
    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(out, CharsetUtil.encoder(CharsetUtil.UTF_8)));
    BodyWriter writer = new BodyWriter(printWriter);

    writer
      .println("<!DOCTYPE html>")
      .println("<html>")
      .println("<head>")
      .print("  <title>").escape(pageTitle).println("</title>")
      .println("    <style type=\"text/css\">")
      .println(style)
      .println("    </style>")
      .println("</head>")
      .println("<body>")
      .println("  <header>")
      .println("    <div class=\"logo\">")
      .println("      <div class=\"martini\">")
      .println("        <h1>Ratpack</h1>")
      .println("      </div>")
      .println("      <p>Development error page</p>")
      .println("    </div>")
      .println("  </header>");

    body.accept(writer);

    writer
      .println("<footer>")
      .println("  <a href=\"http://www.ratpack.io\">Ratpack.io</a>")
      .println("</footer>")
      .println("</body>")
      .println("</html>");

    printWriter.close();
    context.getResponse().send(HttpHeaderConstants.HTML_UTF_8, buffer);
  }

}
