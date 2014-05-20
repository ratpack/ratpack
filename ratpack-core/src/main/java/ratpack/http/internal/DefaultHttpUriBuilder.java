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

package ratpack.http.internal;

import com.google.common.collect.Multimap;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import ratpack.func.Action;
import ratpack.http.HttpUriBuilder;

import java.net.URI;
import java.net.URISyntaxException;

public class DefaultHttpUriBuilder implements HttpUriBuilder {

  private String host;
  private String path;
  private String pathComponent;
  private String protocol;
  private String port;

  private Action<? super  Multimap<String, String>> params;

  private Escaper escaper;


  public DefaultHttpUriBuilder(){
    this.protocol = "http://";
    this.path = "";
    this.pathComponent = "";
    this.port = "";
  }


  @Override
  public HttpUriBuilder secure() {
    this.protocol = "https://";
    return this;
  }

  @Override
  public HttpUriBuilder host(String host) {
    escaper = UrlEscapers.urlFragmentEscaper();
    this.host = escaper.escape(host);
    return this;
  }

  @Override
  public HttpUriBuilder port(int port) {
    this.port = String.format("%s%d", ":", port);
    return this;
  }

  @Override
  public HttpUriBuilder path(String path) {
    escaper = UrlEscapers.urlFragmentEscaper();
    this.path = String.format("/%s/", escaper.escape(path));
    return this;
  }

  @Override
  public HttpUriBuilder pathComponent(String pathComponent) {
    escaper = UrlEscapers.urlPathSegmentEscaper();
    this.pathComponent = escaper.escape(pathComponent);
    return this;
  }

  @Override
  public HttpUriBuilder params(Action<? super Multimap<String, String>> params) throws Exception {
    this.params = params;
    //TODO: implement parameter builder
    return this;
  }

  @Override
  public URI build() throws URISyntaxException {
    URI uri = new URI(this.toString());
    return uri;
  }

  @Override
  public String toString() {
    return protocol + host + port + path + pathComponent;
  }
}
