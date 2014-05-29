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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import ratpack.func.Action;
import ratpack.http.HttpUriBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public class DefaultHttpUriBuilder implements HttpUriBuilder {

  private String host;
  private String path;
  private String pathComponent;
  private String protocol;
  private String queryParams;

  private int port;

  private Multimap<String, String> params;

  private Escaper escaper;


  public DefaultHttpUriBuilder(){
    this.protocol = "http://";
    this.path = "";
    this.pathComponent = "";
    this.port = -1;
    this.params = ArrayListMultimap.create();
    this.queryParams = "";
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
    this.port = port;
    return this;
  }

  @Override
  public HttpUriBuilder path(String path) {
    escaper = UrlEscapers.urlFragmentEscaper();
    this.path +=  "/"+escaper.escape(path);
    return this;
  }

  @Override
  public HttpUriBuilder pathComponent(String pathComponent) {
    escaper = UrlEscapers.urlPathSegmentEscaper();

    if(!this.pathComponent.isEmpty()) {
      this.pathComponent += escaper.escape("/"+pathComponent);
    }else{
      this.pathComponent += String.format("/%s", escaper.escape(pathComponent));
    }
    return this;
  }

  @Override
  public HttpUriBuilder params(Action<? super Multimap<String, String>> params) throws Exception {
    params.execute(this.params);
    buildParameterString();
    return this;
  }

  private void buildParameterString() {
    Set<String> keySet = this.params.keySet();

    for(String key : keySet){

      Object[] paramValues = this.params.get(key).toArray();
      for(Object param : paramValues){
        if(queryParams.equals("")){
          queryParams = String.format("?%s=%s", key, param);
        }else{
          queryParams += String.format("&%s=%s", key, param);
        }
      }
    }
    escaper = UrlEscapers.urlFormParameterEscaper();
    queryParams = escaper.escape(queryParams);
  }

  @Override
  public URI build() throws URISyntaxException {
    URI uri = new URI(this.toString());
    return uri;
  }

  @Override
  public String toString() {

    String uri = protocol + host;
    uri += port != -1  ?  String.format("%s%d", ":", port) : "";
    uri += path + pathComponent + queryParams;

    return uri;
  }
}
