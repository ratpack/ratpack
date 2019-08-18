/*
 * Copyright 2017 the original author or authors.
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

package ratpack.rocker;

import com.google.inject.AbstractModule;
import ratpack.rocker.internal.DefaultRockerRenderer;

/**
 * A guice module that binds {@link RockerRenderer}.
 *
 * @since 1.5
 * @see RockerRenderer
 */
public final class RockerModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(RockerRenderer.class).toInstance(DefaultRockerRenderer.INSTANCE);
  }

}
