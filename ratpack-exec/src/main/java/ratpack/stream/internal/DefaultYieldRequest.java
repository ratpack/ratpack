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

package ratpack.stream.internal;

import ratpack.stream.YieldRequest;

class DefaultYieldRequest implements YieldRequest {
  private final long requestNum;

  private final long subscriptionNum;

  public DefaultYieldRequest(long subscriptionNum, long requestNum) {
    this.subscriptionNum = subscriptionNum;
    this.requestNum = requestNum;
  }

  @Override
  public long getSubscriberNum() {
    return subscriptionNum;
  }

  @Override
  public long getRequestNum() {
    return requestNum;
  }

  @Override
  public String toString() {
    return "YieldRequest{requestNum=" + requestNum + ", subscriptionNum=" + subscriptionNum + '}';
  }
}
