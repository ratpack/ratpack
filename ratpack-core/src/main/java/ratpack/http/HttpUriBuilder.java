/*
 * Copyright 2014 the original author or authors.
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

package ratpack.http;

import com.google.common.collect.Multimap;
import ratpack.func.Action;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by roy on 17/05/14.
 */
public interface HttpUriBuilder {

  HttpUriBuilder secure();
  HttpUriBuilder host(String host); // encodes host name
  HttpUriBuilder port(int port);
  HttpUriBuilder path(String path); // encodes all except / (i.e. can be multiple path components)
  HttpUriBuilder pathComponent(String pathComponent); // encodes all incl. / (i.e. single path component)
  HttpUriBuilder params(Action<? super Multimap<String, String>> params);

  URI build() throws URISyntaxException, MalformedURLException;
}
