/*
 * Copyright 2016 the original author or authors.
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
package ratpack.sse.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;
import ratpack.func.Action;
import ratpack.sse.Event;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ServerSentEventDecoder implements AutoCloseable {

  private static final ByteBuf NEWLINE_BYTEBUF = Unpooled.wrappedBuffer(new byte[]{'\n'});
  private static final char[] EVENT_ID_FIELD_NAME = "event".toCharArray();
  private static final char[] DATA_FIELD_NAME = "data".toCharArray();
  private static final char[] ID_FIELD_NAME = "id".toCharArray();

  private static final ByteProcessor SKIP_TILL_LINE_DELIMITER_PROCESSOR = value -> !isLineDelimiter((char) value);

  private static final ByteProcessor SKIP_LINE_DELIMITERS_AND_SPACES_PROCESSOR = value -> isLineDelimiter((char) value) || (char) value == ' ';

  private static final ByteProcessor SKIP_COLON_AND_WHITE_SPACE_PROCESSOR = value -> {
    char valueChar = (char) value;
    return valueChar == ':' || valueChar == ' ';
  };

  private static final ByteProcessor SCAN_COLON_PROCESSOR = value -> (char) value != ':';

  private static final ByteProcessor SCAN_EOL_PROCESSOR = value -> !isLineDelimiter((char) value);

  private enum State {
    ReadFieldName,
    SkipColonAndWhiteSpaces,
    ReadFieldValue,
    DiscardUntilEOL,
    DiscardEOL,
    Closed
  }

  private enum Type {
    Data,
    Id,
    EventType
  }

  private List<ByteBuf> eventId = new ArrayList<>(1);
  private List<ByteBuf> eventType = new ArrayList<>(1);
  private List<ByteBuf> eventData = new ArrayList<>(1);

  private ByteBuf buffer; // Can be field value of name, according to the current state.

  private Type currentFieldType;
  private State state = State.ReadFieldName;

  private final ByteBufAllocator allocator;
  private final Action<? super Event<?>> emitter;

  public ServerSentEventDecoder(ByteBufAllocator allocator, Action<? super Event<?>> emitter) {
    this.allocator = allocator;
    this.emitter = emitter;
  }

  public void decode(ByteBuf in) throws Exception {
    if (state == State.Closed) {
      in.release();
    }

    try {
      doDecode(in);
    } catch (Exception e) {
      close();
      throw e;
    } finally {
      in.release();
    }
  }

  private void doDecode(ByteBuf in) throws Exception {
    while (in.isReadable()) {
      final int readerIndexAtStart = in.readerIndex();

      switch (state) {
        case SkipColonAndWhiteSpaces:
          if (skipColonAndWhiteSpaces(in)) {
            state = State.ReadFieldValue;
          }
          break;
        case DiscardUntilEOL:
          if (skipTillEOL(in)) {
            state = State.DiscardEOL;
          }
          break;
        case DiscardEOL:
          byte b = in.readByte();
          assert b == '\n';
          state = State.ReadFieldName;
          break;
        case ReadFieldName:
          byte peek = peek(in);
          if (peek == '\n') {
            in.readByte();
            emit();
            break;
          }

          int endOfNameIndex = scanAndFindColon(in);
          if (endOfNameIndex == -1) {
            endOfNameIndex = scanAndFindEndOfLine(in);
          }

          if (endOfNameIndex == -1) {
            // Accumulate data into the field name buffer.
            if (buffer == null) {
              buffer = allocator.buffer();
            }
            // accumulate into incomplete data buffer to be used when the full data arrives.
            buffer.writeBytes(in);
            break;
          }

          int fieldNameLengthInTheCurrentBuffer = endOfNameIndex - readerIndexAtStart;

          ByteBuf fieldNameBuffer;
          if (buffer == null) {
            // Consume the data from the input buffer.
            fieldNameBuffer = allocator.buffer(fieldNameLengthInTheCurrentBuffer, fieldNameLengthInTheCurrentBuffer);
            in.readBytes(fieldNameBuffer, fieldNameLengthInTheCurrentBuffer);
          } else {
            // Read the remaining data into the temporary buffer
            in.readBytes(buffer, fieldNameLengthInTheCurrentBuffer);
            fieldNameBuffer = buffer;
            buffer = null;
          }

          state = State.SkipColonAndWhiteSpaces; // We have read the field name, next we should skip colon & WS.
          try {
            currentFieldType = readCurrentFieldTypeFromBuffer(fieldNameBuffer);
          } finally {
            if (currentFieldType == null) {
              state = State.DiscardUntilEOL; // Ignore this event completely.
            }
            fieldNameBuffer.release();
          }
          break;
        case ReadFieldValue:

          final int endOfLineStartIndex = scanAndFindEndOfLine(in);
          if (endOfLineStartIndex == -1) { // End of line not found, accumulate data into a temporary buffer.
            if (buffer == null) {
              buffer = allocator.buffer(in.readableBytes());
            }
            // accumulate into incomplete data buffer to be used when the full data arrives.
            buffer.writeBytes(in);
          } else { // Read the data till end of line into the value buffer.
            final int bytesAvailableInThisIteration = endOfLineStartIndex - readerIndexAtStart;
            if (buffer == null) {
              buffer = allocator.buffer(bytesAvailableInThisIteration, bytesAvailableInThisIteration);
            }
            buffer.writeBytes(in, bytesAvailableInThisIteration);

            List<ByteBuf> field;
            switch (currentFieldType) {
              case Data:
                field = eventData;
                break;
              case Id:
                field = eventId;
                break;
              default: // type
                field = eventType;
                break;
            }

            field.add(buffer);
            buffer = null;
            state = State.DiscardUntilEOL;
          }
          break;
      }
    }
  }

  private void emit() throws Exception {
    boolean any = false;
    Event<Void> event = new DefaultEvent<>(null);
    String id = str(eventId);
    if (id != null) {
      any = true;
      event.id(id);
    }
    String type = str(eventType);
    if (type != null) {
      any = true;
      event.event(type);
    }
    String data = str(eventData);
    if (data != null) {
      any = true;
      event.data(data);
    }

    if (any) {
      emitter.execute(event);
    }

    state = State.ReadFieldName;
  }

  private String str(List<ByteBuf> bufs) {
    if (bufs.isEmpty()) {
      return null;
    } else {
      String str;
      if (bufs.size() == 1) {
        str = bufs.get(0).toString(StandardCharsets.UTF_8);
      } else {
        CompositeByteBuf composite = allocator.compositeBuffer(bufs.size() * 2 - 1);
        Iterator<ByteBuf> iterator = bufs.iterator();
        composite.addComponent(true, iterator.next());
        while (iterator.hasNext()) {
          composite.addComponent(true, NEWLINE_BYTEBUF.retainedDuplicate());
          composite.addComponent(true, iterator.next());
        }
        str = composite.toString(StandardCharsets.UTF_8);
      }
      bufs.forEach(ByteBuf::release);
      bufs.clear();
      return str;
    }
  }

  private static Type readCurrentFieldTypeFromBuffer(final ByteBuf fieldNameBuffer) {
        /*
         * This code tries to eliminate the need of creating a string from the ByteBuf as the field names are very
         * constrained. The algorithm is as follows:
         *
         * -- Scan the bytes in the buffer.
         * -- Ignore an leading whitespaces
         * -- If the first byte matches the expected field names then use the matching field name char array to verify
         * the rest of the field name.
         * -- If the first byte does not match, reject the field name.
         * -- After the first byte, exact match the rest of the field name with the expected field name, byte by byte.
         * -- If the name does not exactly match the expected value, then reject the field name.
         */
    Type toReturn = Type.Data;
    skipLineDelimiters(fieldNameBuffer);
    int readableBytes = fieldNameBuffer.readableBytes();
    final int readerIndexAtStart = fieldNameBuffer.readerIndex();
    char[] fieldNameToVerify = DATA_FIELD_NAME;
    boolean verified = false;
    int actualFieldNameIndexToCheck = 0; // Starts with 1 as the first char is validated by equality.
    for (int i = readerIndexAtStart; i < readerIndexAtStart + readableBytes; i++) {
      final char charAtI = (char) fieldNameBuffer.getByte(i);

      if (i == readerIndexAtStart) {
        switch (charAtI) { // See which among the known field names this buffer belongs.
          case 'e':
            fieldNameToVerify = EVENT_ID_FIELD_NAME;
            toReturn = Type.EventType;
            break;
          case 'd':
            fieldNameToVerify = DATA_FIELD_NAME;
            toReturn = Type.Data;
            break;
          case 'i':
            fieldNameToVerify = ID_FIELD_NAME;
            toReturn = Type.Id;
            break;
          default:
            return null;
        }
      } else {
        if (++actualFieldNameIndexToCheck >= fieldNameToVerify.length || charAtI != fieldNameToVerify[actualFieldNameIndexToCheck]) {
          // If the character does not match or the buffer is bigger than the expected name, then discard.
          verified = false;
          break;
        } else {
          // Verified till now. If all characters are matching then this stays as verified, else changed to false.
          verified = true;
        }
      }
    }

    if (verified) {
      return toReturn;
    } else {
      return null;
    }
  }

  @Override
  public void close() {
    if (eventId != null) {
      eventId.forEach(ByteBuf::release);
      eventId = null;
    }
    if (eventType != null) {
      eventType.forEach(ByteBuf::release);
      eventType = null;
    }
    if (eventData != null) {
      eventData.forEach(ByteBuf::release);
      eventData = null;
    }

    if (buffer != null) {
      buffer.release();
      buffer = null;
    }

    state = State.Closed;
  }

  private static int scanAndFindColon(ByteBuf byteBuf) {
    return byteBuf.forEachByte(SCAN_COLON_PROCESSOR);
  }

  private static int scanAndFindEndOfLine(ByteBuf byteBuf) {
    return byteBuf.forEachByte(SCAN_EOL_PROCESSOR);
  }

  private static boolean skipLineDelimiters(ByteBuf byteBuf) {
    return skipTillMatching(byteBuf, SKIP_LINE_DELIMITERS_AND_SPACES_PROCESSOR);
  }

  private static boolean skipColonAndWhiteSpaces(ByteBuf byteBuf) {
    return skipTillMatching(byteBuf, SKIP_COLON_AND_WHITE_SPACE_PROCESSOR);
  }

  private static boolean skipTillEOL(ByteBuf in) {
    return skipTillMatching(in, SKIP_TILL_LINE_DELIMITER_PROCESSOR);
  }

  private static boolean skipTillMatching(ByteBuf byteBuf, ByteProcessor processor) {
    final int lastIndexProcessed = byteBuf.forEachByte(processor);
    if (lastIndexProcessed == -1) {
      byteBuf.readerIndex(byteBuf.readerIndex() + byteBuf.readableBytes()); // If all the remaining bytes are to be ignored, discard the buffer.
    } else {
      byteBuf.readerIndex(lastIndexProcessed);
    }

    return lastIndexProcessed != -1;
  }

  private static boolean isLineDelimiter(char c) {
    return c == '\n';
  }

  private static byte peek(ByteBuf buffer) {
    return buffer.getByte(buffer.readerIndex());
  }
}
