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

import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import org.openid4java.message.ParameterList

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static io.netty.handler.codec.http.HttpResponseStatus.OK

class EmbeddedProvider implements Closeable {
  private final Queue<Map> results = new LinkedList<>()
  private Server server
  private PatchedSampleServer sampleServer

  void addResult(Boolean authenticatedAndApproved, String email) {
    results << [authenticatedAndApproved: authenticatedAndApproved, email: email]
  }

  void open() {
    sampleServer = new PatchedSampleServer() {
      @Override
      protected List userInteraction(ParameterList request) {
        def result = results.remove()
        def userSelectedClaimedId = request.getParameterValue("openid.claimed_id")
        return [userSelectedClaimedId, result.authenticatedAndApproved, result.email]
      }
    }
    server = new Server(0)
    server.handler = new AbstractHandler() {
      @Override
      void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        if (request.pathInfo == "/discovery") {
          processDiscovery(response)
        } else {
          sendResponse(sampleServer.processRequest(request, response), response)
        }
      }

      String processDiscovery(HttpServletResponse response) {
        response.setStatus(OK.code())
        response.addHeader("Content-Type", "application/xrds+xml")
        response.outputStream.withWriter {
          it.write("""\
<?xml version="1.0" encoding="UTF-8"?>
<xrds:XRDS xmlns:xrds="xri://\$xrds" xmlns:openid="http://openid.net/xmlns/1.0" xmlns="xri://\$xrd*(\$v*2.0)">
  <XRD version="2.0">
    <Service priority="10">
      <Type>http://specs.openid.net/auth/2.0/signon</Type>
      <Type>http://openid.net/sreg/1.0</Type>
      <Type>http://openid.net/extensions/sreg/1.1</Type>
      <Type>http://schemas.openid.net/pape/policies/2007/06/phishing-resistant</Type>
      <Type>http://openid.net/srv/ax/1.0</Type>
      <URI>${sampleServer.manager.OPEndpointUrl}</URI>
    </Service>
  </XRD>
</xrds:XRDS>"""
          )
        }
      }

      static void sendResponse(String content, HttpServletResponse response) {
        if (content.startsWith("http://")) {
          response.sendRedirect(content)
        } else {
          response.outputStream.withWriter { it.write(content) }
        }
      }
    }
    server.start()
    sampleServer.manager.setOPEndpointUrl("http://localhost:${getPort()}/openid_provider/provider/server/o2")
  }

  @Override
  void close() {
    server.stop()
    clear()
  }

  void clear() {
    results.clear()
  }

  int getPort() {
    server.connectors[0].localPort
  }
}
