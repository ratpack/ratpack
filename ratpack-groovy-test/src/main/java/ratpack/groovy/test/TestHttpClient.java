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

package ratpack.groovy.test;

import com.jayway.restassured.specification.RequestSpecification;
import ratpack.test.ApplicationUnderTest;

public interface TestHttpClient {

  ApplicationUnderTest getApplicationUnderTest();

  RequestSpecification getRequest();

  HttpResponse getResponse();

  RequestSpecification resetRequest();

  HttpResponse head();

  HttpResponse head(String path);

  HttpResponse options();

  HttpResponse options(String path);

  HttpResponse get();

  HttpResponse get(String path);

  String getText();

  String getText(String path);

  HttpResponse post();

  HttpResponse post(String path);

  String postText();

  String postText(String path);

  HttpResponse put();

  HttpResponse put(String path);

  String putText();

  String putText(String path);

  HttpResponse patch();

  HttpResponse patch(String path);

  String patchText();

  String patchText(String path);

  HttpResponse delete();

  HttpResponse delete(String path);

  String deleteText();

  String deleteText(String path);

  RequestSpecification createRequest();
}
