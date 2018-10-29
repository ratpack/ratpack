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

package ratpack.handling.internal;

import ratpack.handling.Context;
import ratpack.handling.Redirector;
import ratpack.http.Request;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.server.PublicAddress;

import java.net.URI;
import java.util.regex.Pattern;

public class DefaultRedirector implements Redirector {

  private static final Pattern ABSOLUTE_PATTERN = Pattern.compile("^https?://.*");

  public static final Redirector INSTANCE = new DefaultRedirector();

  private DefaultRedirector() {
  }

  @Override
  public void redirect(Context context, int code, Object to) {
    String stringValue;
    if (to instanceof URI) {
      stringValue = ((URI) to).toASCIIString();
    } else {
      stringValue = to.toString();
    }
    context.getResponse().status(code);
    String normalizedLocation = generateRedirectLocation(context, context.getRequest(), stringValue);
    context.getResponse().getHeaders().set(HttpHeaderConstants.LOCATION, normalizedLocation);
    context.getResponse().send();
  }

  private String generateRedirectLocation(Context ctx, Request request, String path) {
    //Rules
    //1. Given absolute URL use it
    //1a. Protocol Relative URL given starting of // we use the protocol from the request
    //2. Given Starting Slash prepend public facing domain:port if provided if not use base URL of request
    //3. Given relative URL prepend public facing domain:port plus parent path of request URL otherwise full parent path

    PublicAddress publicAddress = ctx.get(PublicAddress.class);
    String generatedPath;

    if (ABSOLUTE_PATTERN.matcher(path).matches()) {
      //Rule 1 - Path is absolute
      generatedPath = path;
    } else {
      URI host = publicAddress.get();
      if (path.startsWith("//")) {
        //Rule 1a - Protocol relative url
        generatedPath = host.getScheme() + ":" + path;
      } else {
        if (path.startsWith("/")) {
          //Rule 2 - Starting Slash
          generatedPath = host.toString() + path;
        } else {
          //Rule 3
          generatedPath = host.toString() + getParentPath(request.getUri()) + path;
        }
      }
    }

    return generatedPath;
  }

  private String getParentPath(String path) {
    String parentPath = "/";

    int indexOfSlash = path.lastIndexOf('/');
    if (indexOfSlash >= 0) {
      parentPath = path.substring(0, indexOfSlash) + '/';
    }

    if (!parentPath.startsWith("/")) {
      parentPath = "/" + parentPath;
    }
    return parentPath;
  }
}
