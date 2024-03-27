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
package ratpack.core.sse.internal;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;
import ratpack.func.Action;
import ratpack.core.sse.ServerSentEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerSentEventDecoder implements AutoCloseable {

  private static final char[] EVENT_ID_FIELD_NAME = "event".toCharArray();
  private static final char[] DATA_FIELD_NAME = "data".toCharArray();
  private static final char[] ID_FIELD_NAME = "id".toCharArray();

  private static final byte COLON_BYTE = (byte) ':';
  private static final byte NEWLINE_BYTE = (byte) '\n';
  private static final byte SPACE_BYTE = (byte) ' ';

  private static final ByteProcessor IS_NEWLINE_OR_SPACE = value -> value == NEWLINE_BYTE || value == SPACE_BYTE;


  private static final ByteProcessor IS_COLON_OR_SPACE = value -> value == COLON_BYTE || value == SPACE_BYTE;

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

  private List<ByteBuf> idBuffer = new ArrayList<>(1);
  private List<ByteBuf> eventBuffer = new ArrayList<>(1);
  private List<ByteBuf> dataBuffer = new ArrayList<>(1);

  private ByteBuf buffer; // Can be field value of name, according to the current state.

  private Type currentFieldType;
  private State state = State.ReadFieldName;

  private final ByteBufAllocator allocator;
  private final Action<? super ServerSentEvent> emitter;

  public ServerSentEventDecoder(ByteBufAllocator allocator, Action<? super ServerSentEvent> emitter) {
    this.allocator = allocator;
    this.emitter = emitter;
  }

  public void decode(ByteBuf in) throws Exception {
    if (state == State.Closed) {
      in.release();
      return;
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

          int endOfNameIndex = findColon(in);
          if (endOfNameIndex == -1) {
            endOfNameIndex = findNewline(in);
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
            fieldNameBuffer = in.retainedSlice(in.readerIndex(), fieldNameLengthInTheCurrentBuffer);
            in.skipBytes(fieldNameLengthInTheCurrentBuffer);
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

          final int endOfLineStartIndex = findNewline(in);
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
                field = dataBuffer;
                break;
              case Id:
                field = idBuffer;
                break;
              default: // type
                field = eventBuffer;
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
    ServerSentEvent event = ServerSentEvent.builder()
        .id(single(idBuffer))
        .event(single(eventBuffer))
        .unsafeDataLines(multi(dataBuffer))
        .build();

    if (!event.getData().isEmpty() || event.getEvent().isReadable() || event.getId().isReadable()) {
      emitter.execute(event);
    } else {
      event.close();
    }

    state = State.ReadFieldName;
  }

  private static List<ByteBuf> multi(List<ByteBuf> buffers) {
    try {
      if (buffers.isEmpty()) {
        return Collections.emptyList();
      } else if (buffers.size() == 1) {
        return ImmutableList.of(buffers.get(0));
      } else {
        return ImmutableList.copyOf(buffers);
      }
    } finally {
      buffers.clear();
    }
  }

  private static ByteBuf single(List<ByteBuf> bufs) {
    try {
      if (bufs.isEmpty()) {
        return Unpooled.EMPTY_BUFFER;
      } else if (bufs.size() == 1) {
        return bufs.get(0);
      } else {
        throw new IllegalStateException("expected single line but got multi");
      }
    } finally {
      bufs.clear();
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
    skipSpaceAndNewlines(fieldNameBuffer);
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
    if (idBuffer != null) {
      idBuffer.forEach(ByteBuf::release);
      idBuffer = null;
    }
    if (eventBuffer != null) {
      eventBuffer.forEach(ByteBuf::release);
      eventBuffer = null;
    }
    if (dataBuffer != null) {
      dataBuffer.forEach(ByteBuf::release);
      dataBuffer = null;
    }

    if (buffer != null) {
      buffer.release();
      buffer = null;
    }

    state = State.Closed;
  }

  private static int findColon(ByteBuf byteBuf) {
    return find(byteBuf, COLON_BYTE);
  }

  private static int findNewline(ByteBuf byteBuf) {
    return find(byteBuf, NEWLINE_BYTE);
  }

  private static void skipSpaceAndNewlines(ByteBuf byteBuf) {
    skipWhile(byteBuf, IS_NEWLINE_OR_SPACE);
  }

  private static boolean skipColonAndWhiteSpaces(ByteBuf byteBuf) {
    return skipWhile(byteBuf, IS_COLON_OR_SPACE);
  }

  private static boolean skipTillEOL(ByteBuf in) {
    return skipUntil(in, NEWLINE_BYTE);
  }

  private static boolean skipWhile(ByteBuf byteBuf, ByteProcessor processor) {
    final int lastIndexProcessed = byteBuf.forEachByte(processor);
    if (lastIndexProcessed == -1) {
      byteBuf.readerIndex(byteBuf.writerIndex());
    } else {
      byteBuf.readerIndex(lastIndexProcessed);
    }

    return lastIndexProcessed != -1;
  }

  private static boolean skipUntil(ByteBuf byteBuf, byte target) {
    int indexOf = find(byteBuf, target);
    if (indexOf == -1) {
      byteBuf.readerIndex(byteBuf.writerIndex());
      return false;
    } else {
      byteBuf.readerIndex(indexOf);
      return true;
    }
  }

  private static int find(ByteBuf byteBuf, byte target) {
    return byteBuf.indexOf(byteBuf.readerIndex(), byteBuf.writerIndex(), target);
  }

  private static byte peek(ByteBuf buffer) {
    return buffer.getByte(buffer.readerIndex());
  }
}
