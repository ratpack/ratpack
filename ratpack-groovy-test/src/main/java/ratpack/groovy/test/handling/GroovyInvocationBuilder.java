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

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.func.Action;
import ratpack.handling.Handler;
import ratpack.launch.LaunchConfigBuilder;
import ratpack.registry.RegistryBuilder;
import ratpack.registry.RegistrySpec;
import ratpack.test.handling.Invocation;
import ratpack.test.handling.InvocationBuilder;
import ratpack.test.handling.InvocationTimeoutException;

import java.nio.file.Path;
import java.util.Map;

public interface GroovyInvocationBuilder extends InvocationBuilder {

  GroovyInvocationBuilder registry(@DelegatesTo(value = RegistryBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

  @Override
  Invocation invoke(Handler handler) throws InvocationTimeoutException;

  @Override
  GroovyInvocationBuilder header(String name, String value);

  @Override
  GroovyInvocationBuilder body(byte[] bytes, String contentType);

  @Override
  GroovyInvocationBuilder body(String text, String contentType);

  @Override
  GroovyInvocationBuilder responseHeader(String name, String value);

  @Override
  GroovyInvocationBuilder method(String method);

  @Override
  GroovyInvocationBuilder uri(String uri);

  @Override
  GroovyInvocationBuilder timeout(int timeout);

  @Override
  GroovyInvocationBuilder registry(Action<? super RegistrySpec> action) throws Exception;

  @Override
  GroovyInvocationBuilder register(Object object);

  @Override
  GroovyInvocationBuilder pathBinding(Map<String, String> pathTokens);

  @Override
  GroovyInvocationBuilder pathBinding(String boundTo, String pastBinding, Map<String, String> pathTokens);

  @Override
  GroovyInvocationBuilder launchConfig(Path baseDir, Action<? super LaunchConfigBuilder> action) throws Exception;

  @Override
  GroovyInvocationBuilder launchConfig(Action<? super LaunchConfigBuilder> action) throws Exception;
}
