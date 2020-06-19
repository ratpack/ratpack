/*
 * Copyright 2015 the original author or authors.
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

package ratpack.core.handling.internal;

import io.netty.util.AsciiString;
import ratpack.core.handling.RequestId;
import ratpack.core.http.Request;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class UuidBasedRequestIdGenerator implements RequestId.Generator {

  public static final RequestId.Generator INSTANCE = new UuidBasedRequestIdGenerator();

  @Override
  public RequestId generate(Request request) {
    Random random = ThreadLocalRandom.current();
    return RequestId.of(new AsciiString(new UUID(random.nextLong(), random.nextLong()).toString()));
  }
}
