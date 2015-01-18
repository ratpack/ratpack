/*
 * Copyright 2015 the original author or authors.
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
import io.netty.buffer.ByteBufProcessor;
import ratpack.func.Action;
import ratpack.sse.Event;

import static io.netty.util.CharsetUtil.UTF_8;

/**
 * Decodes {@link io.netty.buffer.ByteBuf}s into {@link ratpack.sse.Event}s.
 * <p>
 * ByteBufs are parsed as detailed by the <a href="http://www.w3.org/TR/eventsource/">W3C Server-Sent Events Specification</a>.  Inspiration was also taken from
 * <a href="https://github.com/ReactiveX/RxNetty/blob/0.x/rxnetty/src/main/java/io/reactivex/netty/protocol/http/sse/ServerSentEventDecoder.java">RxNetty's decoder</a>
 * <p>
 * A single ByteBuf may result in 0 or more events being dispatched to the supplied event action.
 */
public class ServerSentEventDecoder {
  private static final char LINE_FEED = '\n';
  private static final char CARRIAGE_RETURN = '\r';
  private static final char[] EVENT_ID_FIELD_NAME = "event".toCharArray();
  private static final char[] DATA_FIELD_NAME = "data".toCharArray();
  private static final char[] ID_FIELD_NAME = "id".toCharArray();
  private static final ByteBufProcessor SCAN_EOL_PROCESSOR = value -> !isLineDelimiter((char) value);
  private static final ByteBufProcessor SCAN_COLON_PROCESSOR = value -> (char) value != ':';
  private static final ByteBufProcessor SKIP_COLON_AND_WHITE_SPACE_PROCESSOR = value -> {
    char valueChar = (char) value;
    return valueChar == ':' || valueChar == ' ';
  };

  public static final ServerSentEventDecoder INSTANCE = new ServerSentEventDecoder();

  private ServerSentEventDecoder() {}

  private enum State {
    SkipColonAndWhiteSpaces, // Skip colon and all whitespaces after reading field name.
    SkipTillEOL, // Skip to the end of the current line.
    ReadFieldName, // Read till a colon to get the name of the field.
    ReadFieldValue, // Read value till the line delimiter.
    ReadLine, // Read till the next line delimeter.
    DispatchEvent // Dispatch a new server sent event with the current values.
  }

  private enum FieldName {
    Event,
    Data,
    Id
  }

  public void decode(ByteBuf in, ByteBufAllocator bufferAllocator, Action<Event<?>> eventAction) throws Exception {
    ByteBuf currentLineBuffer = null;
    String eventData = null;
    String eventType = null;
    String eventId = null;
    FieldName currentFieldType = null;
    State state = State.ReadLine;

    try {
      while (in.isReadable()) {
        final int readerIndexAtStart = in.readerIndex();

        switch (state) {
          case ReadLine:
            final int endOfLineStartIndex = scanAndFindEndOfLine(in);

            if (-1 == endOfLineStartIndex) { // End of line not found, set readIndex to the end
              in.readerIndex(in.capacity());
            } else {
              final int bytesAvailableInThisLine = endOfLineStartIndex - readerIndexAtStart;

              if (bytesAvailableInThisLine == 0) {
                state = State.DispatchEvent;
              } else {
                currentLineBuffer = bufferAllocator.buffer(bytesAvailableInThisLine, bytesAvailableInThisLine);
                in.readBytes(currentLineBuffer, bytesAvailableInThisLine);
                state = State.ReadFieldName;
              }
            }

            break;
          case DispatchEvent:
            eventAction.execute(new DefaultEvent<>().id(eventId).event(eventType).data(eventData));

            eventId = null;
            eventType = null;
            eventData = null;

            state = State.SkipTillEOL;

            break;
          case ReadFieldName:
            int indexOfColon = scanAndFindColon(currentLineBuffer);
            if (-1 == indexOfColon) { // No colon found, use the whole line as the field name, and the empty string as the field value.
              indexOfColon = currentLineBuffer.capacity();
            }

            if (1 == indexOfColon) { // Line starts with a colon, ignore
              state = State.ReadLine;
              currentLineBuffer.release();
            } else {
              ByteBuf fieldNameBuffer = bufferAllocator.buffer(indexOfColon, indexOfColon);
              currentLineBuffer.readBytes(fieldNameBuffer, indexOfColon);
              state = State.SkipColonAndWhiteSpaces; // We have read the field name, next we should skip colon & WS.

              try {
                currentFieldType = readCurrentFieldTypeFromBuffer(fieldNameBuffer);
              } finally {
                if (null == currentFieldType) {
                  state = State.SkipTillEOL; // Ignore this event completely.
                }
                fieldNameBuffer.release();
              }
            }

            break;
          case SkipColonAndWhiteSpaces:
            skipColonAndWhiteSpaces(currentLineBuffer);
            state = State.ReadFieldValue;

            break;
          case SkipTillEOL:
            skipTilEOL(in);
            state = State.ReadLine;
            break;
          case ReadFieldValue:
            final int bytesAvailableInThisValue = currentLineBuffer.readableBytes();
            ByteBuf currentFieldValue = bufferAllocator.buffer(bytesAvailableInThisValue, bytesAvailableInThisValue);
            currentLineBuffer.readBytes(currentFieldValue, bytesAvailableInThisValue);
            state = State.SkipTillEOL;

            switch (currentFieldType) {
              case Data:
                if (null == eventData) {
                  eventData = currentFieldValue.toString(UTF_8);
                } else {
                  eventData = eventData + LINE_FEED + currentFieldValue.toString(UTF_8);
                }
                break;
              case Id:
                eventId = currentFieldValue.toString(UTF_8);
                break;
              case Event:
                eventType = currentFieldValue.toString(UTF_8);
                break;
            }

            currentFieldValue.release();
            currentLineBuffer.release();

            break;

        }

      }
    } finally {
      in.release();
    }
  }

  private static int scanAndFindColon(ByteBuf byteBuf) {
    return byteBuf.forEachByte(SCAN_COLON_PROCESSOR);
  }

  private static int scanAndFindEndOfLine(ByteBuf byteBuf) {
    return byteBuf.forEachByte(SCAN_EOL_PROCESSOR);
  }

  private static boolean skipColonAndWhiteSpaces(ByteBuf byteBuf) {
    return skipTillMatching(byteBuf, SKIP_COLON_AND_WHITE_SPACE_PROCESSOR);
  }

  private static boolean skipTilEOL(ByteBuf byteBuf) {
    if (skipLineFeed(byteBuf)) {
      return skipCarriageReturn(byteBuf);
    }

    return skipCarriageReturn(byteBuf) && skipLineFeed(byteBuf);
  }

  private static boolean skipLineFeed(ByteBuf byteBuf) {
    return skipTillFirstMatching(byteBuf, (byte) LINE_FEED);
  }

  private static boolean skipCarriageReturn(ByteBuf byteBuf) {
    return skipTillFirstMatching(byteBuf, (byte) CARRIAGE_RETURN);
  }

  private static boolean skipTillMatching(ByteBuf byteBuf, ByteBufProcessor processor) {
    final int lastIndexProcessed = byteBuf.forEachByte(processor);
    if (-1 == lastIndexProcessed) {
      byteBuf.readerIndex(byteBuf.readerIndex() + byteBuf.readableBytes()); // If all the remaining bytes are to be ignored, discard the buffer.
    } else {
      byteBuf.readerIndex(lastIndexProcessed);
    }
    return -1 != lastIndexProcessed;
  }

  private static boolean skipTillFirstMatching(ByteBuf byteBuf, byte thing) {
    int i = byteBuf.indexOf(byteBuf.readerIndex(), byteBuf.capacity(), thing);
    if (-1 == i) {
      return false;
    } else {
      byteBuf.readByte();
      return true;
    }
  }

  /**
   * This code tries to eliminate the need of creating a string from the ByteBuf as the field names are very
   * constrained. The algorithm is as follows:
   *
   * -- Scan the bytes in the buffer.
   * -- If the first byte matches the expected field names then use the matching field name char array to verify
   * the rest of the field name.
   * -- If the first byte does not match, reject the field name.
   * -- After the first byte, exact match the rest of the field name with the expected field name, byte by byte.
   * -- If the name does not exactly match the expected value, then reject the field name.
   */
  private static FieldName readCurrentFieldTypeFromBuffer(final ByteBuf fieldNameBuffer) {

    FieldName toReturn = FieldName.Data;
    int readableBytes = fieldNameBuffer.readableBytes();
    final int readerIndexAtStart = fieldNameBuffer.readerIndex();
    char[] fieldNameToVerify = DATA_FIELD_NAME;
    boolean verified = false;
    int actualFieldNameIndexToCheck = 0; // Starts with 1 as the first char is validated by equality.
    for (int i = readerIndexAtStart; i < readableBytes; i++) {
      final char charAtI = (char) fieldNameBuffer.getByte(i);
      if (i == readerIndexAtStart) {
        switch (charAtI) { // See which among the known field names this buffer belongs.
          case 'e':
            fieldNameToVerify = EVENT_ID_FIELD_NAME;
            toReturn = FieldName.Event;
            break;
          case 'd':
            fieldNameToVerify = DATA_FIELD_NAME;
            toReturn = FieldName.Data;
            break;
          case 'i':
            fieldNameToVerify = ID_FIELD_NAME;
            toReturn = FieldName.Id;
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

  private static boolean isLineDelimiter(char c) {
    return c == CARRIAGE_RETURN || c == LINE_FEED;
  }

}
