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

package ratpack.core.handling.internal;

import ratpack.core.handling.Context;
import ratpack.core.handling.Redirector;
import ratpack.core.http.internal.HttpHeaderConstants;

import java.net.URI;

public class DefaultRedirector implements Redirector {

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
    context.getResponse().getHeaders().set(HttpHeaderConstants.LOCATION, stringValue);
    context.getResponse().send();
  }

}
