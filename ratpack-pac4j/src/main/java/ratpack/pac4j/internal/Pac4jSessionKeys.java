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

package ratpack.pac4j.internal;

import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.profile.UserProfile;
import ratpack.session.SessionKey;

public class Pac4jSessionKeys {

  public static final SessionKey<String> REQUESTED_URL = SessionKey.of(
    Pac4jConstants.REQUESTED_URL,
    String.class
  );

  public static final SessionKey<UserProfile> USER_PROFILE = SessionKey.of(
    Pac4jConstants.USER_PROFILE,
    UserProfile.class
  );

  private Pac4jSessionKeys() {
  }

}
