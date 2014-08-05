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
import ratpack.util.MultiValueMap;

import java.net.URI;
import java.util.Map;

public interface HttpUrlSpec {

  HttpUrlSpec secure();

  HttpUrlSpec host(String host);

  HttpUrlSpec port(int port);

  HttpUrlSpec path(String path);

  HttpUrlSpec pathSegment(String pathComponent);

  HttpUrlSpec params(String... params);

  HttpUrlSpec params(Map<String, String> params);

  HttpUrlSpec params(Multimap<String, String> params);

  HttpUrlSpec params(MultiValueMap<String, String> params);

  HttpUrlSpec set(URI uri);

  URI getURL();

}
