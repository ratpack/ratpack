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

package ratpack.groovy.test.handling;

import com.google.common.net.HostAndPort;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.func.Action;
import ratpack.launch.LaunchConfigBuilder;
import ratpack.registry.RegistryBuilder;
import ratpack.registry.RegistrySpec;
import ratpack.test.handling.RequestFixture;

import java.nio.file.Path;
import java.util.Map;

/**
 * A more Groovy friendly version of {@link RequestFixture}.
 *
 * @see ratpack.groovy.test.GroovyUnitTest#requestFixture()
 */
public interface GroovyRequestFixture extends RequestFixture {

  /**
   * A closure friendly overload of {@link #registry(Action)}.
   *
   * @param closure the registry configuration
   * @return this
   * @see #registry(Action)
   */
  GroovyRequestFixture registry(@DelegatesTo(value = RegistryBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture header(String name, String value);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture body(byte[] bytes, String contentType);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture body(String text, String contentType);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture responseHeader(String name, String value);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture method(String method);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture uri(String uri);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture timeout(int timeoutSeconds);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture registry(Action<? super RegistrySpec> action) throws Exception;

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture pathBinding(Map<String, String> pathTokens);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture pathBinding(String boundTo, String pastBinding, Map<String, String> pathTokens);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture launchConfig(Path baseDir, Action<? super LaunchConfigBuilder> action) throws Exception;

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture launchConfig(Action<? super LaunchConfigBuilder> action) throws Exception;

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture remoteAddress(HostAndPort remote);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture localAddress(HostAndPort local);

}
