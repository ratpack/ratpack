/*
 * Copyright 2019 the original author or authors.
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

import ratpack.handling.Handler;
import ratpack.http.Request;

/**
 * An interface for creating a Ratpack {@link Handler} based on the contents of the received {@link Request}.
 * <p>
 * This interface is useful in creating mocks for remote APIs in tests.
 * @since 1.7.0
 */
@FunctionalInterface
public interface HandlerFactory {

  Handler receive(Request request);
}
