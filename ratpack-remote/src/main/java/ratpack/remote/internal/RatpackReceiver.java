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

package ratpack.remote.internal;

import io.remotecontrol.groovy.server.ClosureCommandRunner;
import io.remotecontrol.groovy.server.ContextFactory;
import io.remotecontrol.result.Result;
import io.remotecontrol.result.impl.DefaultResultFactory;
import io.remotecontrol.server.MultiTypeReceiver;
import io.remotecontrol.server.Receiver;
import ratpack.registry.RegistrySpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RatpackReceiver implements Receiver {

  private final Receiver delegate;

  public RatpackReceiver(ContextFactory contextFactory) {
    ClassLoader classLoader = getClass().getClassLoader();
    delegate = new MultiTypeReceiver(classLoader, new ClosureCommandRunner(classLoader, contextFactory, new DefaultResultFactory() {
      @Override
      protected Result forUnserializable(Object unserializable) {
        if (unserializable instanceof RegistrySpec) {
          return super.forValue(null);
        } else {
          return super.forUnserializable(unserializable);
        }
      }
    }));
  }

  @Override
  public void execute(InputStream commandStream, OutputStream resultStream) throws IOException {
    delegate.execute(commandStream, resultStream);
  }
}
