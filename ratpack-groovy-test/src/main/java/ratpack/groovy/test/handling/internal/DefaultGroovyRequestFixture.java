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

package ratpack.groovy.test.handling.internal;

import com.google.common.net.HostAndPort;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.func.Action;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.groovy.test.handling.GroovyRequestFixture;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.registry.RegistryBuilder;
import ratpack.registry.RegistrySpec;
import ratpack.server.ServerConfigBuilder;
import ratpack.test.handling.HandlingResult;
import ratpack.test.handling.RequestFixture;
import ratpack.test.http.MultipartFileSpec;
import ratpack.test.http.MultipartFormSpec;

import java.util.Map;

public class DefaultGroovyRequestFixture implements GroovyRequestFixture {

  private final RequestFixture delegate;

  @Override
  public GroovyRequestFixture registry(@DelegatesTo(value = RegistryBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    ClosureUtil.configureDelegateFirst(getRegistry(), closure);
    return this;
  }

  public DefaultGroovyRequestFixture(RequestFixture delegate) {
    this.delegate = delegate;
  }

  @Override
  public HandlingResult handle(Handler handler) {
    return delegate.handle(handler);
  }

  @Override
  public HandlingResult handleChain(Action<? super Chain> chainAction) throws Exception {
    return delegate.handleChain(chainAction);
  }

  @Override
  public GroovyRequestFixture header(CharSequence name, String value) {
    delegate.header(name, value);
    return this;
  }

  @Override
  public GroovyRequestFixture body(byte[] bytes, String contentType) {
    delegate.body(bytes, contentType);
    return this;
  }

  @Override
  public GroovyRequestFixture body(String text, String contentType) {
    delegate.body(text, contentType);
    return this;
  }

  @Override
  public MultipartFileSpec file() {
    return delegate.file();
  }

  @Override
  public GroovyRequestFixture file(String field, String filename, String data) {
    delegate.file(field, filename, data);
    return this;
  }

  @Override
  public MultipartFormSpec form() {
    return delegate.form();
  }

  @Override
  public GroovyRequestFixture form(Map<String, String> data) {
    delegate.form(data);
    return this;
  }

  @Override
  public GroovyRequestFixture responseHeader(CharSequence name, String value) {
    delegate.responseHeader(name, value);
    return this;
  }

  @Override
  public GroovyRequestFixture method(String method) {
    delegate.method(method);
    return this;
  }

  @Override
  public GroovyRequestFixture uri(String uri) {
    delegate.uri(uri);
    return this;
  }

  @Override
  public GroovyRequestFixture timeout(int timeoutSeconds) {
    delegate.timeout(timeoutSeconds);
    return this;
  }

  @Override
  public RegistrySpec getRegistry() {
    return delegate.getRegistry();
  }

  @Override
  public GroovyRequestFixture registry(Action<? super RegistrySpec> action) throws Exception {
    delegate.registry(action);
    return this;
  }

  @Override
  public GroovyRequestFixture pathBinding(Map<String, String> pathTokens) {
    delegate.pathBinding(pathTokens);
    return this;
  }

  @Override
  public GroovyRequestFixture pathBinding(String boundTo, String pastBinding, Map<String, String> pathTokens) {
    delegate.pathBinding(boundTo, pastBinding, pathTokens);
    return this;
  }

  @Override
  public GroovyRequestFixture pathBinding(String boundTo, String pastBinding, Map<String, String> pathTokens, String description) {
    delegate.pathBinding(boundTo, pastBinding, pathTokens, description);
    return this;
  }

  @Override
  public GroovyRequestFixture serverConfig(Action<? super ServerConfigBuilder> action) throws Exception {
    delegate.serverConfig(action);
    return this;
  }

  @Override
  public GroovyRequestFixture remoteAddress(HostAndPort remote) {
    delegate.remoteAddress(remote);
    return this;
  }

  @Override
  public GroovyRequestFixture localAddress(HostAndPort local) {
    delegate.localAddress(local);
    return this;
  }

  @Override
  public GroovyRequestFixture protocol(String protocol) {
    delegate.protocol(protocol);
    return this;
  }
}
