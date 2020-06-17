/*
 * Copyright 2016 the original author or authors.
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

package ratpack.core.service.internal;

import ratpack.core.service.LegacyServiceAdapter;
import ratpack.core.service.Service;
import ratpack.core.service.StartEvent;
import ratpack.core.service.StopEvent;

@SuppressWarnings("deprecation")
public class DefaultLegacyServiceAdapter implements Service, LegacyServiceAdapter {

  private final ratpack.core.server.Service delegate;

  public DefaultLegacyServiceAdapter(ratpack.core.server.Service delegate) {
    this.delegate = delegate;
  }

  @Override
  public ratpack.core.server.Service getAdapted() {
    return delegate;
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public void onStart(StartEvent event) throws Exception {
    delegate.onStart(adaptEvent(event));
  }

  @Override
  public void onStop(StopEvent event) throws Exception {
    delegate.onStop(adaptEvent(event));
  }

  private ratpack.core.server.StartEvent adaptEvent(StartEvent event) {
    if (event instanceof ratpack.core.server.StartEvent) {
      return (ratpack.core.server.StartEvent) event;
    } else {
      return new DefaultEvent(event.getRegistry(), event.isReload());
    }
  }

  private ratpack.core.server.StopEvent adaptEvent(StopEvent event) {
    if (event instanceof ratpack.core.server.StopEvent) {
      return (ratpack.core.server.StopEvent) event;
    } else {
      return new DefaultEvent(event.getRegistry(), event.isReload());
    }
  }
}
