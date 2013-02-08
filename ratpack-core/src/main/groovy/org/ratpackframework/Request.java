/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.Cookie;
import org.ratpackframework.http.MediaType;
import org.ratpackframework.session.Session;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A request to be handled.
 */
public interface Request {

  String getMethod();

  String getUri();

  String getQuery();

  String getPath();

  String getText();

  ChannelBuffer getBuffer();

  Map<String, List<String>> getQueryParams();

  Map<String, String> getUrlParams();

  Session getSession();

  MediaType getContentType();

  Map<String, List<String>> getForm();

  Set<Cookie> getCookies();

}
