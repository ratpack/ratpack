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

package ratpack.test.internal.testname

import groovy.transform.PackageScope

final class CurrentTestName {

    private static volatile Optional<String> name = Optional.empty()

    private CurrentTestName() {
    }

    static Optional<String> get() {
        name
    }

    @PackageScope
    static void set(Optional<String> name) {
        CurrentTestName.name = name
    }

}
