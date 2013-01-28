package org.ratpackframework.templating.internal;

import org.jboss.netty.buffer.ChannelBuffer;
import org.vertx.java.core.buffer.Buffer;

import java.io.IOException;

/**
 * Note: harcoded to expect UTF-8.
 */
public class TemplateParser {

  public void parse(Buffer input, Buffer output) throws IOException {
    parse(input.getChannelBuffer(), output);
  }

  private void parse(ChannelBuffer input, Buffer script) throws IOException {
    startScript(script);
    byte c;
    while (input.readable()) {
      c = input.readByte();
      if (c == '<') {
        input.markReaderIndex();
        c = input.readByte();
        if (c != '%') {
          script.appendString("<");
          input.resetReaderIndex();
        } else {
          input.markReaderIndex();
          c = input.readByte();
          if (c == '=') {
            groovyExpression(input, script);
          } else {
            input.resetReaderIndex();
            groovySection(input, script);
          }
        }
        continue; // at least '<' is consumed ... read next chars.
      }
      if (c == '$') {
        input.markReaderIndex();
        c = input.readByte();
        if (c != '{') {
          script.appendString("$");
          input.resetReaderIndex();
        } else {
          input.markReaderIndex();
          processGSstring(input, script);
        }
        continue; // at least '$' is consumed ... read next chars.
      }
      if (c == '\"') {
        script.appendString("\\");
      }

      // Handle raw new line characters.
      if (c == '\n' || c == '\r') {
        if (c == '\r') { // on Windows, "\r\n" is a new line.
          input.markReaderIndex();
          c = input.readByte();
          if (c != '\n') {
            input.resetReaderIndex();
          }
        }
        script.appendString("\n");
        continue;
      }
      script.appendByte(c);
    }

    endScript(script);
  }

  private void startScript(Buffer script) {
    script.appendString("str(\"\"\"");
  }

  private void endScript(Buffer script) {
    script.appendString("\"\"\")");
  }

  private void processGSstring(ChannelBuffer input, Buffer output) throws IOException {
    output.appendString("${");
    byte c;
    while (input.readable()) {
      c = input.readByte();
      if (c != '\n' && c != '\r') {
        output.appendByte(c);
      }
      if (c == '}') {
        break;
      }
    }
  }

  private void groovyExpression(ChannelBuffer input, Buffer output) throws IOException {
    output.appendString("${");
    byte c;
    while (input.readable()) {
      c = input.readByte();
      if (c == '%') {
        c = input.readByte();
        if (c != '>') {
          output.appendString("%");
        } else {
          break;
        }
      }
      if (c != '\n' && c != '\r') {
        output.appendByte(c);
      }
    }
    output.appendString("}");
  }

  private void groovySection(ChannelBuffer input, Buffer output) throws IOException {
    output.appendString("\"\"\");");
    byte c;
    while (input.readable()) {
      c = input.readByte();
      if (c == '%') {
        c = input.readByte();
        if (c != '>') {
          output.appendString("%");
        } else {
          break;
        }
      }

      output.appendByte(c);
    }

    output.appendString(";\nstr(\"\"\"");
  }

}
