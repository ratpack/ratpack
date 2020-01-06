/*
 * Copyright 2020 the original author or authors.
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

package ratpack.micrometer.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.apache.commons.lang3.StringUtils;
import ratpack.handling.Context;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.http.Status;

import java.util.function.BiFunction;
import java.util.regex.Pattern;

public final class HandlerTags {
  public static final BiFunction<Context, Throwable, Tags> RECOMMENDED_TAGS = (context, exception) -> Tags.of(
    HandlerTags.method(context.getRequest()), HandlerTags.uri(context),
    HandlerTags.exception(exception), HandlerTags.status(context.getResponse()), HandlerTags.outcome(context.getResponse()));

  private static final Tag URI_NOT_FOUND = Tag.of("uri", "NOT_FOUND");

  private static final Tag URI_REDIRECTION = Tag.of("uri", "REDIRECTION");

  private static final Tag URI_ROOT = Tag.of("uri", "root");

  private static final Tag URI_UNKNOWN = Tag.of("uri", "UNKNOWN");

  private static final Tag EXCEPTION_NONE = Tag.of("exception", "None");

  private static final Tag STATUS_UNKNOWN = Tag.of("status", "UNKNOWN");

  private static final Tag METHOD_UNKNOWN = Tag.of("method", "UNKNOWN");

  private static final Pattern LEADING_SLASH_PATTERN = Pattern.compile("^/");

  private static final Pattern TRAILING_SLASH_PATTERN = Pattern.compile("/$");

  private static final Pattern MULTIPLE_SLASH_PATTERN = Pattern.compile("//+");

  private HandlerTags() {
  }

  /**
   * Creates a {@code method} tag based on the {@link Request#getMethod()
   * method} of the given {@code request}.
   * @param request the request
   * @return the method tag whose value is a capitalized method (e.g. GET).
   */
  public static Tag method(Request request) {
    return (request != null) ? Tag.of("method", request.getMethod().getName()) : METHOD_UNKNOWN;
  }

  /**
   * Creates a {@code status} tag based on the status of the given {@code response}.
   * @param response the HTTP response
   * @return the status tag derived from the status of the response
   */
  public static Tag status(Response response) {
    return (response != null) ? Tag.of("status", Integer.toString(response.getStatus().getCode())) : STATUS_UNKNOWN;
  }

  /**
   * Creates a {@code uri} tag based on the URI of the given {@code request}. Falling back to {@code REDIRECTION} for 3xx responses,
   * {@code NOT_FOUND} for 404 responses, {@code root} for requests with no path info, and {@code UNKNOWN} for all other requests.
   * @param context the request's context
   * @return the uri tag derived from the request
   */
  public static Tag uri(Context context) {
    if (context.getRequest() != null) {
      Response response = context.getResponse();
      if (response != null) {
        Status status = response.getStatus();
        if (status != null) {
          if (status.is3xx()) {
            return URI_REDIRECTION;
          }
          if (status == Status.NOT_FOUND) {
            return URI_NOT_FOUND;
          }
        }
      }

      String pathInfo = getPathInfo(context);
      if (context.getPathBinding().getBoundTo().isEmpty()) {
        return URI_NOT_FOUND;
      } else if (pathInfo.isEmpty()) {
        return URI_ROOT;
      } else {
        return Tag.of("uri", pathInfo);
      }
    }
    return URI_UNKNOWN;
  }

  private static String getPathInfo(Context context) {
    String pathInfo = context.getPathBinding().getDescription();
    String uri = StringUtils.isNotBlank(pathInfo) ? pathInfo : "/";
    uri = MULTIPLE_SLASH_PATTERN.matcher(uri).replaceAll("/");
    uri = LEADING_SLASH_PATTERN.matcher(uri).replaceAll("");
    return TRAILING_SLASH_PATTERN.matcher(uri).replaceAll("");
  }

  /**
   * Creates a {@code exception} tag based on the {@link Class#getSimpleName() simple
   * name} of the class of the given {@code exception}.
   * @param exception the exception, may be {@code null}
   * @return the exception tag derived from the exception
   */
  public static Tag exception(Throwable exception) {
    if (exception != null) {
      String simpleName = exception.getClass().getSimpleName();
      return Tag.of("exception", StringUtils.isNotBlank(simpleName) ? simpleName : exception.getClass().getName());
    }
    return EXCEPTION_NONE;
  }

  /**
   * Creates an {@code outcome} tag based on the status of the given {@code response}.
   * @param response the HTTP response
   * @return the outcome tag derived from the status of the response
   */
  public static Tag outcome(Response response) {
    Outcome outcome = (response != null) ? Outcome.forStatus(response.getStatus().getCode()) : Outcome.UNKNOWN;
    return outcome.asTag();
  }

  /**
   * The outcome of an HTTP request.
   */
  public enum Outcome {

    /**
     * Outcome of the request was informational.
     */
    INFORMATIONAL,

    /**
     * Outcome of the request was success.
     */
    SUCCESS,

    /**
     * Outcome of the request was redirection.
     */
    REDIRECTION,

    /**
     * Outcome of the request was client error.
     */
    CLIENT_ERROR,

    /**
     * Outcome of the request was server error.
     */
    SERVER_ERROR,

    /**
     * Outcome of the request was unknown.
     */
    UNKNOWN;

    private final Tag tag;

    Outcome() {
      this.tag = Tag.of("outcome", name());
    }

    /**
     * Returns the {@code Outcome} as a {@link Tag} named {@code outcome}.
     * @return the {@code outcome} {@code Tag}
     */
    public Tag asTag() {
      return this.tag;
    }

    /**
     * Return the {@code Outcome} for the given HTTP {@code status} code.
     * @param status the HTTP status code
     * @return the matching Outcome
     */
    public static Outcome forStatus(int status) {
      if (status >= 100 && status < 200) {
        return INFORMATIONAL;
      }
      else if (status >= 200 && status < 300) {
        return SUCCESS;
      }
      else if (status >= 300 && status < 400) {
        return REDIRECTION;
      }
      else if (status >= 400 && status < 500) {
        return CLIENT_ERROR;
      }
      else if (status >= 500 && status < 600) {
        return SERVER_ERROR;
      }
      return UNKNOWN;
    }
  }
}
