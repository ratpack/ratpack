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

public interface GroovyRequestFixture extends RequestFixture {

  GroovyRequestFixture registry(@DelegatesTo(value = RegistryBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

  @Override
  GroovyRequestFixture header(String name, String value);

  @Override
  GroovyRequestFixture body(byte[] bytes, String contentType);

  @Override
  GroovyRequestFixture body(String text, String contentType);

  @Override
  GroovyRequestFixture responseHeader(String name, String value);

  @Override
  GroovyRequestFixture method(String method);

  @Override
  GroovyRequestFixture uri(String uri);

  @Override
  GroovyRequestFixture timeout(int timeoutSeconds);

  @Override
  GroovyRequestFixture registry(Action<? super RegistrySpec> action) throws Exception;

  @Override
  GroovyRequestFixture pathBinding(Map<String, String> pathTokens);

  @Override
  GroovyRequestFixture pathBinding(String boundTo, String pastBinding, Map<String, String> pathTokens);

  @Override
  GroovyRequestFixture launchConfig(Path baseDir, Action<? super LaunchConfigBuilder> action) throws Exception;

  @Override
  GroovyRequestFixture launchConfig(Action<? super LaunchConfigBuilder> action) throws Exception;

  @Override
  GroovyRequestFixture setRemoteAddress(HostAndPort remote);

  @Override
  GroovyRequestFixture setLocalAddress(HostAndPort local);
}
