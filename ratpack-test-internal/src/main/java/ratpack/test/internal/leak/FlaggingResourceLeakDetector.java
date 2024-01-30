/*
 * Copyright 2021 the original author or authors.
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

package ratpack.test.internal.leak;

import io.netty.util.ResourceLeakDetector;

import java.util.Queue;

public class FlaggingResourceLeakDetector<T> extends ResourceLeakDetector<T> {

    private final Queue<String> leaks;

    FlaggingResourceLeakDetector(Class<T> resourceType, int samplingInterval, Queue<String> leaks) {
        super(resourceType, samplingInterval);
        this.leaks = leaks;
    }

    @Override
    protected void reportTracedLeak(String resourceType, String records) {
      leaks.add(records);
    }

}
