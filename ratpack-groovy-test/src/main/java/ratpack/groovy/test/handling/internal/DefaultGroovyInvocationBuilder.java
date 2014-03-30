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

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.func.Action;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.groovy.test.handling.GroovyInvocationBuilder;
import ratpack.handling.Handler;
import ratpack.registry.RegistryBuilder;
import ratpack.registry.RegistrySpec;
import ratpack.test.handling.Invocation;
import ratpack.test.handling.InvocationBuilder;
import ratpack.test.handling.InvocationTimeoutException;

import java.util.Map;

public class DefaultGroovyInvocationBuilder implements GroovyInvocationBuilder {

  private final InvocationBuilder delegate;

  @Override
  public GroovyInvocationBuilder registry(@DelegatesTo(value = RegistryBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    ClosureUtil.configureDelegateFirst(getRegistry(), closure);
    return this;
  }

  public DefaultGroovyInvocationBuilder(InvocationBuilder delegate) {
    this.delegate = delegate;
  }

  @Override
  public Invocation invoke(Handler handler) throws InvocationTimeoutException {
    return delegate.invoke(handler);
  }

  @Override
  public GroovyInvocationBuilder header(String name, String value) {
    delegate.header(name, value);
    return this;
  }

  @Override
  public GroovyInvocationBuilder body(byte[] bytes, String contentType) {
    delegate.body(bytes, contentType);
    return this;
  }

  @Override
  public GroovyInvocationBuilder body(String text, String contentType) {
    delegate.body(text, contentType);
    return this;
  }

  @Override
  public GroovyInvocationBuilder responseHeader(String name, String value) {
    delegate.responseHeader(name, value);
    return this;
  }

  @Override
  public GroovyInvocationBuilder method(String method) {
    delegate.method(method);
    return this;
  }

  @Override
  public GroovyInvocationBuilder uri(String uri) {
    delegate.uri(uri);
    return this;
  }

  @Override
  public GroovyInvocationBuilder timeout(int timeout) {
    delegate.timeout(timeout);
    return this;
  }

  @Override
  public RegistrySpec getRegistry() {
    return delegate.getRegistry();
  }

  @Override
  public GroovyInvocationBuilder registry(Action<? super RegistrySpec> action) throws Exception {
    delegate.registry(action);
    return this;
  }

  @Override
  public GroovyInvocationBuilder register(Object object) {
    delegate.register(object);
    return this;
  }

  @Override
  public GroovyInvocationBuilder pathBinding(Map<String, String> pathTokens) {
    delegate.pathBinding(pathTokens);
    return this;
  }

  @Override
  public GroovyInvocationBuilder pathBinding(String boundTo, String pastBinding, Map<String, String> pathTokens) {
    delegate.pathBinding(boundTo, pastBinding, pathTokens);
    return this;
  }

}
