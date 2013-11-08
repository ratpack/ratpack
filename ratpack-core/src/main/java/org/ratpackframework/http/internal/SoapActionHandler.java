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

package org.ratpackframework.http.internal;

import org.ratpackframework.handling.Handler;

public class SoapActionHandler extends HeaderHandler {

  private static final String SOAP_ACTION_HTTP_HEADER_NAME = "SOAPAction";

  public SoapActionHandler(String headerValue, Handler handler) {
    super(headerValue, handler);
  }

  @Override
  protected String getHeaderName() {
    return SOAP_ACTION_HTTP_HEADER_NAME;
  }

}
