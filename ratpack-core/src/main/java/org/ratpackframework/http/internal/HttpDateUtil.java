/*
 * ====================================================================
 *
 *  Copyright 1999-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.ratpackframework.http.internal;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A utility class for parsing and formatting HTTP dates as used in cookies and other headers.  This class handles dates as defined by RFC 2616 section 3.3.1 as well as some other common non-standard
 * formats.
 *
 * @author Christopher Brown
 * @author Michael Becke
 */
public class HttpDateUtil {

  /**
   * Date format pattern used to parse HTTP date headers in RFC 1123 format.
   */
  public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

  /**
   * Date format pattern used to parse HTTP date headers in RFC 1036 format.
   */
  public static final String PATTERN_RFC1036 = "EEEE, dd-MMM-yy HH:mm:ss zzz";

  /**
   * Date format pattern used to parse HTTP date headers in ANSI C <code>asctime()</code> format.
   */
  public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";

  private static final Collection DEFAULT_PATTERNS = Arrays.asList(PATTERN_ASCTIME, PATTERN_RFC1036, PATTERN_RFC1123);

  private static final Date DEFAULT_TWO_DIGIT_YEAR_START;

  static {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2000, Calendar.JANUARY, 1, 0, 0);
    DEFAULT_TWO_DIGIT_YEAR_START = calendar.getTime();
  }

  private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

  /**
   * Parses a date value.  The formats used for parsing the date value are retrieved from the default http params.
   *
   * @param dateValue the date value to parse
   * @return the parsed date
   * @throws HttpDateParseException if the value could not be parsed using any of the supported date formats
   */
  public static Date parseDate(String dateValue) throws HttpDateParseException {
    return parseDate(dateValue, null, null);
  }

  /**
   * Parses the date value using the given date formats.
   *
   * @param dateValue the date value to parse
   * @param dateFormats the date formats to use
   * @return the parsed date
   * @throws HttpDateParseException if none of the dataFormats could parse the dateValue
   */
  public static Date parseDate(String dateValue, Collection dateFormats)
      throws HttpDateParseException {
    return parseDate(dateValue, dateFormats, null);
  }

  /**
   * Parses the date value using the given date formats.
   *
   * @param dateValue the date value to parse
   * @param dateFormats the date formats to use
   * @param startDate During parsing, two digit years will be placed in the range <code>startDate</code> to <code>startDate + 100 years</code>. This value may be <code>null</code>. When
   * <code>null</code> is given as a parameter, year <code>2000</code> will be used.
   * @return the parsed date
   * @throws HttpDateParseException if none of the dataFormats could parse the dateValue
   */
  public static Date parseDate(
      String dateValue,
      Collection dateFormats,
      Date startDate
  ) throws HttpDateParseException {

    if (dateValue == null) {
      throw new IllegalArgumentException("dateValue is null");
    }
    if (dateFormats == null) {
      dateFormats = DEFAULT_PATTERNS;
    }
    if (startDate == null) {
      startDate = DEFAULT_TWO_DIGIT_YEAR_START;
    }
    // trim single quotes around date if present
    // see issue #5279
    if (dateValue.length() > 1
        && dateValue.startsWith("'")
        && dateValue.endsWith("'")
        ) {
      dateValue = dateValue.substring(1, dateValue.length() - 1);
    }

    SimpleDateFormat dateParser = null;

    for (Object dateFormat : dateFormats) {
      String format = (String) dateFormat;
      if (dateParser == null) {
        dateParser = new SimpleDateFormat(format, Locale.US);
        dateParser.setTimeZone(TimeZone.getTimeZone("GMT"));
        dateParser.set2DigitYearStart(startDate);
      } else {
        dateParser.applyPattern(format);
      }
      try {
        return dateParser.parse(dateValue);
      } catch (ParseException pe) {
        // ignore this exception, we will try the next format
      }
    }

    // we were unable to parse the date
    throw new HttpDateParseException("Unable to parse the date " + dateValue);
  }

  /**
   * Formats the given date according to the RFC 1123 pattern.
   *
   * @param date The date to format.
   * @return An RFC 1123 formatted date string.
   * @see #PATTERN_RFC1123
   */
  public static String formatDate(Date date) {
    return formatDate(date, PATTERN_RFC1123);
  }

  /**
   * Formats the given date according to the specified pattern.  The pattern must conform to that used by the {@link SimpleDateFormat simple date format} class.
   *
   * @param date The date to format.
   * @param pattern The pattern to use for formatting the date.
   * @return A formatted date string.
   * @throws IllegalArgumentException If the given date pattern is invalid.
   * @see SimpleDateFormat
   */
  public static String formatDate(Date date, String pattern) {
    if (date == null) {
      throw new IllegalArgumentException("date is null");
    }
    if (pattern == null) {
      throw new IllegalArgumentException("pattern is null");
    }

    SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.US);
    formatter.setTimeZone(GMT);
    return formatter.format(date);
  }

  /**
   * This class should not be instantiated.
   */
  private HttpDateUtil() {
  }

}
