package org.ratpackframework.groovy.templating.internal;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.util.CharsetUtil;

import java.io.IOException;

/**
 * Note: hardcoded to expect UTF-8.
 */
public class TemplateParser {

  private static byte[] bytes(String s) {
    return s.getBytes(CharsetUtil.UTF_8);
  }

  private static final byte[] LESS_THAN = bytes("<");
  private static final byte[] DOLLAR_BRACE = bytes("${");
  private static final byte[] PERCENT = bytes("%");
  private static final byte[] START_OUTPUT = bytes("$(\n\"\"\"");
  private static final byte[] START_CODE = bytes("\"\"\"\n);");
  private static final byte[] CLOSE_BRACE = bytes("}");
  private static final byte[] DOLLAR = bytes("$");
  private static final byte[] BACKSLASH = bytes("\\");
  private static final byte[] SEMICOLON = bytes(";");

  public void parse(ChannelBuffer input, ChannelBuffer output) throws IOException {
    startScript(output);
    byte c;
    while (input.readable()) {
      c = input.readByte();
      if (c == '<') {
        input.markReaderIndex();
        c = input.readByte();
        if (c != '%') {
          output.writeBytes(LESS_THAN);
          input.resetReaderIndex();
        } else {
          input.markReaderIndex();
          c = input.readByte();
          if (c == '=') {
            groovyExpression(input, output);
          } else {
            input.resetReaderIndex();
            groovySection(input, output);
          }
        }
        continue; // at least '<' is consumed ... read next chars.
      }
      if (c == '$') {
        input.markReaderIndex();
        c = input.readByte();
        if (c != '{') {
          output.writeBytes(DOLLAR);
          input.resetReaderIndex();
        } else {
          input.markReaderIndex();
          processGSstring(input, output);
        }
        continue; // at least '$' is consumed ... read next chars.
      }
      if (c == '\"') {
        output.writeBytes(BACKSLASH);
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
        output.writeByte('\n');
        continue;
      }
      output.writeByte(c);
    }

    endScript(output);
  }

  private void startScript(ChannelBuffer output) {
    output.writeBytes(START_OUTPUT);
  }

  private void endScript(ChannelBuffer output) {
    output.writeBytes(START_CODE);
  }

  private void processGSstring(ChannelBuffer input, ChannelBuffer output) throws IOException {
    output.writeBytes(DOLLAR_BRACE);
    byte c;
    while (input.readable()) {
      c = input.readByte();
      if (c != '\n' && c != '\r') {
        output.writeByte(c);
      }
      if (c == '}') {
        break;
      }
    }
  }

  private void groovyExpression(ChannelBuffer input, ChannelBuffer output) throws IOException {
    output.writeBytes(DOLLAR_BRACE);
    byte c;
    while (input.readable()) {
      c = input.readByte();
      if (c == '%') {
        c = input.readByte();
        if (c != '>') {
          output.writeBytes(PERCENT);
        } else {
          break;
        }
      }
      if (c != '\n' && c != '\r') {
        output.writeByte(c);
      }
    }
    output.writeBytes(CLOSE_BRACE);
  }

  private void groovySection(ChannelBuffer input, ChannelBuffer output) throws IOException {
    output.writeBytes(START_CODE);
    output.writeByte('\n');
    byte c;
    while (input.readable()) {
      c = input.readByte();
      if (c == '%') {
        c = input.readByte();
        if (c != '>') {
          output.writeBytes(PERCENT);
        } else {
          break;
        }
      }

      output.writeByte(c);
    }

    output.writeByte('\n');
    output.writeBytes(SEMICOLON);
    output.writeBytes(START_OUTPUT);
  }


}
