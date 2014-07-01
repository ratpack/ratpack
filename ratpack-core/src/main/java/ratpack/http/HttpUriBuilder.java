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

import java.net.URI;
import java.util.Map;

public interface HttpUriBuilder {

  HttpUriBuilder secure();

  HttpUriBuilder host(String host);

  HttpUriBuilder port(int port);

  HttpUriBuilder path(String path);

  HttpUriBuilder pathSegment(String pathComponent);

  HttpUriBuilder params(String... params);

  HttpUriBuilder params(Map<String, String> params);

  HttpUriBuilder params(Multimap<String, String> params);

  URI build();

}
