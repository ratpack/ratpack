/*
 * Copyright 2020 the original author or authors.
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

/**
 * This package provides two forms of config for each registry. For registry implementation Foo,
 * there will be a RatpackFooConfig that allows the registry to be configured in the Ratpack
 * {@link ratpack.server.ServerConfig} without assuming that Foo's {@link io.micrometer.core.instrument.MeterRegistry}
 * implementation is present on the classpath. RatpackFooConfig is mapped to MappedFooConfig just
 * before FooMeterRegistry is instantiated, and implements the FooConfig interface from the
 * corresponding Micrometer dependency.
 */
package ratpack.micrometer.metrics.config;
