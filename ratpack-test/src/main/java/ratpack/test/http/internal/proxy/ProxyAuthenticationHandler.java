/*
 * Copyright 2022 the original author or authors.
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

package ratpack.test.http.internal.proxy;

import com.google.common.net.HttpHeaders;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.core.http.client.ProxyCredentials;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

public class ProxyAuthenticationHandler extends SimpleChannelInboundHandler<HttpRequest> {
  public static final Logger LOGGER = LoggerFactory.getLogger(TestHttpProxyServer.class);

  private final ProxyCredentials proxyCredentials;

  public ProxyAuthenticationHandler(  ProxyCredentials proxyCredentials) {
    this.proxyCredentials = proxyCredentials;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
    String basicAuthToken = request.headers().get(HttpHeaders.PROXY_AUTHORIZATION);
    if (basicAuthToken == null || basicAuthToken.isEmpty()) {
      respondProxyAuthenticationRequired(ctx);
    } else {
      String encodedValue = basicAuthToken.split(" ")[1];
      String decodedValue = new String(Base64.getDecoder().decode(encodedValue), ISO_8859_1);
      if (decodedValue.equals(proxyCredentials.getUsername() + ":" + proxyCredentials.getPassword())) {
        ReferenceCountUtil.retain(request);
        ctx.fireChannelRead(request);
      } else {
        respondProxyAuthenticationRequired(ctx);
      }
    }
  }

  private void respondProxyAuthenticationRequired(ChannelHandlerContext ctx) {
    ctx.writeAndFlush(proxyAuthenticationRequiredResponse()).addListener(ChannelFutureListener.CLOSE);
  }

  private HttpResponse proxyAuthenticationRequiredResponse() {
    HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED);
    response.headers().add(HttpHeaders.PROXY_AUTHENTICATE, "Basic");
    return response;
  }

}
