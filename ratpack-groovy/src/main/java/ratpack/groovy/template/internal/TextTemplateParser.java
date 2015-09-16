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

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

import java.io.IOException;

/**
 * Note: hardcoded to expect UTF-8.
 */
public class TextTemplateParser {

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

  public void parse(ByteBuf input, ByteBuf output) throws IOException {
    startScript(output);
    byte c;
    while (input.isReadable()) {
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

  private void startScript(ByteBuf output) {
    output.writeBytes(START_OUTPUT);
  }

  private void endScript(ByteBuf output) {
    output.writeBytes(START_CODE);
  }

  private void processGSstring(ByteBuf input, ByteBuf output) throws IOException {
    output.writeBytes(DOLLAR_BRACE);
    byte c;
    while (input.isReadable()) {
      c = input.readByte();
      if (c != '\n' && c != '\r') {
        output.writeByte(c);
      }
      if (c == '}') {
        break;
      }
    }
  }

  private void groovyExpression(ByteBuf input, ByteBuf output) throws IOException {
    output.writeBytes(DOLLAR_BRACE);
    byte c;
    while (input.isReadable()) {
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

  private void groovySection(ByteBuf input, ByteBuf output) throws IOException {
    output.writeBytes(START_CODE);
    output.writeByte('\n');
    byte c;
    while (input.isReadable()) {
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
