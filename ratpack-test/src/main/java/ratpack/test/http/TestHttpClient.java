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

package ratpack.test.http;

import com.google.common.collect.ImmutableMultimap;
import io.netty.handler.codec.http.Cookie;
import ratpack.func.Action;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.test.ApplicationUnderTest;

import java.util.List;

public interface TestHttpClient {

  ApplicationUnderTest getApplicationUnderTest();

  void requestSpec(Action<? super RequestSpec> requestAction);

  void params(Action<? super ImmutableMultimap.Builder<String, Object>> params);

  void resetRequest();

  ReceivedResponse getResponse();

  ReceivedResponse head();

  ReceivedResponse head(String path);

  ReceivedResponse options();

  ReceivedResponse options(String path);

  ReceivedResponse get();

  ReceivedResponse get(String path);

  String getText();

  String getText(String path);

  ReceivedResponse post();

  ReceivedResponse post(String path);

  String postText();

  String postText(String path);

  ReceivedResponse put();

  ReceivedResponse put(String path);

  String putText();

  String putText(String path);

  ReceivedResponse patch();

  ReceivedResponse patch(String path);

  String patchText();

  String patchText(String path);

  ReceivedResponse delete();

  ReceivedResponse delete(String path);

  String deleteText();

  String deleteText(String path);

  List<Cookie> getCookies();
}
