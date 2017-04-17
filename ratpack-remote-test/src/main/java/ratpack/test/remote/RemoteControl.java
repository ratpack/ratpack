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

package ratpack.test.remote;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import io.remotecontrol.CommandChain;
import io.remotecontrol.client.CommandGenerator;
import io.remotecontrol.client.RemoteControlSupport;
import io.remotecontrol.client.Transport;
import io.remotecontrol.client.UnserializableResultStrategy;
import io.remotecontrol.groovy.ClosureCommand;
import io.remotecontrol.groovy.client.ClosureCommandGenerator;
import io.remotecontrol.groovy.client.RawClosureCommand;
import io.remotecontrol.result.Result;
import io.remotecontrol.transport.http.HttpTransport;
import ratpack.remote.CommandDelegate;
import ratpack.test.ApplicationUnderTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static ratpack.remote.RemoteControl.DEFAULT_REMOTE_CONTROL_PATH;

public class RemoteControl {

  private final RemoteControlSupport<ClosureCommand> support;
  private final List<Closure<?>> uses;
  private final CommandGenerator<RawClosureCommand, ClosureCommand> generator = new ClosureCommandGenerator();

  private final static class DelegatingTransport implements Transport {

    private final ApplicationUnderTest applicationUnderTest;
    private final String path;

    private DelegatingTransport(ApplicationUnderTest applicationUnderTest, String path) {
      this.applicationUnderTest = applicationUnderTest;
      this.path = path;
    }

    @Override
    public Result send(CommandChain<?> commandChain) throws IOException {
      return new HttpTransport(applicationUnderTest.getAddress() + path).send(commandChain);
    }
  }

  public RemoteControl(ApplicationUnderTest application, String path, UnserializableResultStrategy unserializableResultStrategy) {
    this(createSupport(application, path, unserializableResultStrategy), Collections.emptyList());
  }

  public static RemoteControlSupport<ClosureCommand> createSupport(ApplicationUnderTest application, String path, UnserializableResultStrategy unserializableResultStrategy) {
    return new RemoteControlSupport<>(new DelegatingTransport(application, path), unserializableResultStrategy, Thread.currentThread().getContextClassLoader());
  }

  public RemoteControl(ApplicationUnderTest application, String path) {
    this(application, path, UnserializableResultStrategy.THROW);
  }

  public RemoteControl(ApplicationUnderTest application, UnserializableResultStrategy unserializableResultStrategy) {
    this(application, DEFAULT_REMOTE_CONTROL_PATH, unserializableResultStrategy);
  }

  public RemoteControl(ApplicationUnderTest application) {
    this(application, DEFAULT_REMOTE_CONTROL_PATH, UnserializableResultStrategy.THROW);
  }

  private RemoteControl(RemoteControlSupport<ClosureCommand> support, List<Closure<?>> uses) {
    this.support = support;
    this.uses = uses;
  }

  public RemoteControl uses(Closure<?>... uses) {
    return new RemoteControl(support, Arrays.asList(uses));
  }

  public Object exec(@DelegatesTo(value = CommandDelegate.class, strategy = Closure.DELEGATE_FIRST) Closure<?>... commands) throws IOException {
    List<ClosureCommand> closureCommands = new ArrayList<>();
    for (Closure<?> command : commands) {
      ClosureCommand closureCommand = generator.generate(new RawClosureCommand(command, uses));
      closureCommands.add(closureCommand);
    }

    return support.send(CommandChain.of(ClosureCommand.class, closureCommands));
  }

  public static Closure<?> command(@DelegatesTo(value = CommandDelegate.class, strategy = Closure.DELEGATE_FIRST) Closure<?> command) {
    return command.dehydrate();
  }

}
