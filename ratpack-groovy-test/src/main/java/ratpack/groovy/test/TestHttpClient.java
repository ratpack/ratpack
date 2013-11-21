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

import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import ratpack.test.ApplicationUnderTest;

public interface TestHttpClient {

  ApplicationUnderTest getApplicationUnderTest();

  RequestSpecification getRequest();

  Response getResponse();

  RequestSpecification resetRequest();

  Response head();

  Response head(String path);

  Response options();

  Response options(String path);

  Response get();

  Response get(String path);

  String getText();

  String getText(String path);

  Response post();

  Response post(String path);

  String postText();

  String postText(String path);

  Response put();

  Response put(String path);

  String putText();

  String putText(String path);

  Response delete();

  Response delete(String path);

  String deleteText();

  String deleteText(String path);

  RequestSpecification createRequest();
}
