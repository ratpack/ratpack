/*
 * Copyright 2014 the original author or authors.
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
 * Integration with <a href="http://www.pac4j.org/">pac4j</a> for authentication/authorization.
 * <p>
 * This module requires both the {@code ratpack-guice} and {@code ratpack-session} modules.
 *
 * To use this module:
 * <ol>
 *   <li>Add session support to your application, if not already present.  See the {@code ratpack-session} module for more details.</li>
 *   <li>Add the ratpack-pac4j module ({@code io.ratpack:ratpack-pac4j:VERSION}) to your build definition.</li>
 *   <li>
 *     Add the appropriate pac4j module to your build definition.  For example, to use <a href="http://openid.net/">OpenID</a> authentication you would add pac4j-openid
 *     ({@code org.pac4j:pac4j-openid:VERSION}).
 *   </li>
 *   <li>
 *     Register either {@link ratpack.pac4j.Pac4jModule} or {@link ratpack.pac4j.InjectedPac4jModule} with Guice, providing it with an appropriate {@link org.pac4j.core.client.Client} and
 *     {@link ratpack.pac4j.Authorizer}.
 *   </li>
 * </ol>
 * </p>
 */
package ratpack.pac4j;
