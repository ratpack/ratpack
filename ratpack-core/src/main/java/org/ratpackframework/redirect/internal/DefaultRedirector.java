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

package org.ratpackframework.redirect.internal;

import io.netty.handler.codec.http.HttpHeaders;
import org.ratpackframework.handling.Context;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;
import org.ratpackframework.redirect.Redirector;
import org.ratpackframework.url.PublicAddress;

import java.net.URL;
import java.util.regex.Pattern;

public class DefaultRedirector implements Redirector {

  private static final Pattern ABSOLUTE_PATTERN = Pattern.compile("^https?://.*");

  public void redirect(Context context, Response response, Request request, String location, int code) {
    response.status(code);
    response.setHeader(HttpHeaders.Names.LOCATION, generateRedirectLocation(context, request, location));
    response.send();

  }

  private String generateRedirectLocation(Context context, Request request, String path) {
    //Rules
    //1. Given absolute URL use it
    //2. Given Starting Slash prepend public facing domain:port if provided if not use base URL of request
    //3. Given relative URL prepend public facing domain:port plus parent path of request URL otherwise full parent path

    PublicAddress publicAddress = context.get(PublicAddress.class);
    String generatedPath = null;
    URL host = publicAddress.getUrl(context);

    if (ABSOLUTE_PATTERN.matcher(path).matches()) {
      //Rule 1 - Path is absolute
      generatedPath = path;
    } else {
      if (path.charAt(0) == '/') {
        //Rule 2 - Starting Slash
        generatedPath = host.toString() + path;
      } else {
        //Rule 3
        generatedPath = host.toString() + getParentPath(request.getUri()) + path;
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
