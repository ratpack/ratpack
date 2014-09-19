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

package ratpack.test.handling;

import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.launch.LaunchConfigBuilder;
import ratpack.registry.RegistrySpec;

import java.nio.file.Path;
import java.util.Map;

/**
 * Convenient super class for {@link RequestFixture} configuration actions.
 *
 * @see #execute()
 */
public abstract class RequestFixtureAction implements Action<RequestFixture>, RequestFixture {

  private RequestFixture requestFixture;

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestFixture body(byte[] bytes, String contentType) {
    return getRequestFixture().body(bytes, contentType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestFixture body(String text, String contentType) {
    return getRequestFixture().body(text, contentType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RegistrySpec getRegistry() {
    return getRequestFixture().getRegistry();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HandlingResult handle(Handler handler) throws HandlerTimeoutException {
    return getRequestFixture().handle(handler);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HandlingResult handle(Action<? super Chain> chainAction) throws Exception {
    return getRequestFixture().handle(chainAction);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestFixture header(String name, String value) {
    return getRequestFixture().header(name, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestFixture launchConfig(Path baseDir, Action<? super LaunchConfigBuilder> action) throws Exception {
    return getRequestFixture().launchConfig(baseDir, action);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestFixture launchConfig(Action<? super LaunchConfigBuilder> action) throws Exception {
    return getRequestFixture().launchConfig(action);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestFixture method(String method) {
    return getRequestFixture().method(method);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestFixture pathBinding(Map<String, String> pathTokens) {
    return getRequestFixture().pathBinding(pathTokens);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestFixture pathBinding(String boundTo, String pastBinding, Map<String, String> pathTokens) {
    return getRequestFixture().pathBinding(boundTo, pastBinding, pathTokens);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestFixture registry(Action<? super RegistrySpec> action) throws Exception {
    return getRequestFixture().registry(action);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestFixture responseHeader(String name, String value) {
    return getRequestFixture().responseHeader(name, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestFixture timeout(int timeoutSeconds) {
    return getRequestFixture().timeout(timeoutSeconds);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestFixture uri(String uri) {
    return getRequestFixture().uri(uri);
  }

  /**
   * Delegates to {@link #execute()}, using the given {@code requestFixture} for delegation.
   *
   * @param requestFixture the request fixture to configure
   * @throws Exception Any thrown by {@link #execute()}
   */
  public final void execute(RequestFixture requestFixture) throws Exception {
    try {
      this.requestFixture = requestFixture;
      execute();
    } finally {
      this.requestFixture = null;
    }
  }

  /**
   * Implementations can naturally use the {@link RequestFixture} DSL for the duration of this method.
   *
   * @throws Exception and exception thrown while configuring the request fixture
   */
  protected abstract void execute() throws Exception;

  protected RequestFixture getRequestFixture() throws IllegalStateException {
    if (requestFixture == null) {
      throw new IllegalStateException("no requestFixture set - RequestFixture methods should only be called during execute()");
    }
    return requestFixture;
  }
}
