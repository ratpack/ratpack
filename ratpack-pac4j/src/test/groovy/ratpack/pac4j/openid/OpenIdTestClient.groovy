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

package ratpack.pac4j.openid

import org.openid4java.message.AuthSuccess
import org.openid4java.message.ax.FetchRequest
import org.openid4java.message.ax.FetchResponse
import org.pac4j.core.client.BaseClient
import org.pac4j.core.context.WebContext
import org.pac4j.openid.client.BaseOpenIdClient
import org.pac4j.openid.profile.yahoo.YahooOpenIdProfile

import static org.openid4java.message.ax.AxMessage.OPENID_NS_AX
import static org.pac4j.openid.profile.yahoo.YahooOpenIdAttributesDefinition.EMAIL

class OpenIdTestClient extends BaseOpenIdClient<YahooOpenIdProfile> {
  final boolean directRedirection = true

  private final int providerPort

  OpenIdTestClient(int providerPort) {
    this.providerPort = providerPort
  }

  @Override
  protected String getUser(WebContext context) {
    return "http://localhost:${providerPort}/discovery"
  }

  @Override
  protected FetchRequest getFetchRequest() {
    def fetchRequest = FetchRequest.createFetchRequest()
    fetchRequest.addAttribute(EMAIL, "http://axschema.org/contact/email", true)
    return fetchRequest
  }

  @Override
  protected YahooOpenIdProfile createProfile(AuthSuccess authSuccess) {
    def profile = new YahooOpenIdProfile()
    if (authSuccess.hasExtension(OPENID_NS_AX)) {
        def fetchResp = authSuccess.getExtension(OPENID_NS_AX) as FetchResponse
        profile.addAttribute(EMAIL, fetchResp.getAttributeValue(EMAIL))
    }
    return profile
  }

  @Override
  protected BaseClient newClient() {
    return new OpenIdTestClient(providerPort)
  }
}
