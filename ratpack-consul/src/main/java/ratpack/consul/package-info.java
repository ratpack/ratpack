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

/**
 * Support for integrating Ratpack applications with Hashicorp's <a href=https://www.consul.io/>Consul</a> service discovery and distributed configuration engine.
 * <h3>Key Features</h3>
 * <p>The Consul support provides the following key features:</p>
 * <ul>
 * <li>Support for reading data from Consul's distributed Key-Value store as a {@link com.google.common.io.ByteSource}.</li>
 * </ul>
 * <h3>Using Consul Key-Value store to provide application configuration</h3>
 * <p>
 * The Consul integration provides support for reading values from Consul's Key-Value store as a {@code ByteSource}.
 * The {@code ByteSource} can then be provided to Ratpack's configuration parsing methods:
 * <ul>
 * <li>{@link ratpack.server.ServerConfigBuilder#json(com.google.common.io.ByteSource)}</li>
 * <li>{@link ratpack.server.ServerConfigBuilder#props(com.google.common.io.ByteSource)}</li>
 * <li>{@link ratpack.server.ServerConfigBuilder#yaml(com.google.common.io.ByteSource)}</li>
 * </ul>
 * </p>
 *
 * @since 1.5
 */
package ratpack.consul;
