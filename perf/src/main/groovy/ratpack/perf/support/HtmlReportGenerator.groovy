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

package ratpack.perf.support

import groovy.xml.MarkupBuilder

abstract class HtmlReportGenerator {

  static generate(InputStream resultsJson, OutputStream destination) {
    def out = new OutputStreamWriter(destination)
    out << "<!DOCTYPE html>\n"
    def builder = new MarkupBuilder(out)
    builder.html {
      doHead(delegate as MarkupBuilder, resultsJson)
      doBody(delegate as MarkupBuilder)
    }
  }

  static doHead(MarkupBuilder builder, InputStream resultsJson) {
    builder.head {
      meta('http-equiv': "content-type", content: "text/html; charset=utf-8")
      script(type: "text/javascript") {
        mkp.yieldUnescaped("\n//<![CDATA[\n$ResourceLoader.jquery\n//]]>\n")
      }
      script(type: "text/javascript") {
        mkp.yieldUnescaped("\n//<![CDATA[\n$ResourceLoader.tableBarChartJs\n//]]>\n")
      }
      style {
        mkp.yieldUnescaped("\n$ResourceLoader.tableBarChartCss\n")
      }
      script(type: "text/javascript") {
        mkp.yieldUnescaped("\n//<![CDATA[\nvar resultData = ${resultsJson.text}\n//]]>\n")
      }
      script(type: "text/javascript") {
        mkp.yieldUnescaped('''\n//<![CDATA[\n
          $(function() {
            var tableBody = $("table#data tbody");
            var endpoints = resultData.endpoints;
            $.each(endpoints, function(name, data) {
              var results = data.results;
              var baseNum = (results.base.averageBatchTime).toFixed(5);
              var headNum = (results.head.averageBatchTime).toFixed(5);
              var diff = (headNum - baseNum).toFixed(5)

              $("<tr>")
                .append("<th>" + name + "</th>")
                .append("<td>" + baseNum + "</td>")
                .append("<td>" + headNum + "</td>")
                .appendTo(tableBody);
            });

            $('#data').tableBarChart('#chart', 'Performance', true);
          });
        //]]>\n''')
      }
    }
  }

  static doBody(MarkupBuilder builder) {
    builder.body {
      div(id: "chart", style: "width: 800px; height: 400px;", "")
      table(id: 'data') {
        thead {
          tr {
            th ""
            th "Base"
            th "Head"
          }
        }
        tbody {}
      }
    }
  }

}
