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

package ratpack.service.internal;

import ratpack.service.Service;
import ratpack.service.StartEvent;
import ratpack.service.StopEvent;

@SuppressWarnings("deprecation")
public class DefaultLegacyServiceAdapter implements Service, ratpack.service.LegacyServiceAdapter {

  private final ratpack.server.Service delegate;

  public DefaultLegacyServiceAdapter(ratpack.server.Service delegate) {
    this.delegate = delegate;
  }

  @Override
  public ratpack.server.Service getAdapted() {
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

  private ratpack.server.StartEvent adaptEvent(StartEvent event) {
    if (event instanceof ratpack.server.StartEvent) {
      return (ratpack.server.StartEvent) event;
    } else {
      return new DefaultEvent(event.getRegistry(), event.isReload());
    }
  }

  private ratpack.server.StopEvent adaptEvent(StopEvent event) {
    if (event instanceof ratpack.server.StopEvent) {
      return (ratpack.server.StopEvent) event;
    } else {
      return new DefaultEvent(event.getRegistry(), event.isReload());
    }
  }
}
