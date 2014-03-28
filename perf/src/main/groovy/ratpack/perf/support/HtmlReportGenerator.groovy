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
      body {}
    }
  }

  static doHead(MarkupBuilder builder, InputStream resultsJson) {
    builder.head {
      meta('http-equiv': "content-type", content: "text/html; charset=utf-8")
      script(type: "text/javascript") {
        mkp.yieldUnescaped("\n//<![CDATA[\n$ResourceLoader.jquery\n//]]>\n")
      }
      script(type: "text/javascript") {
        mkp.yieldUnescaped("\n//<![CDATA[\n$ResourceLoader.jqPlotJs\n//]]>\n")
      }
      style {
        mkp.yieldUnescaped("\n$ResourceLoader.jqplotCss\n")
      }
      script(type: "text/javascript") {
        mkp.yieldUnescaped("\n//<![CDATA[\nvar resultData = ${resultsJson.text}\n//]]>\n")
      }
      script(type: "text/javascript") {
        mkp.yieldUnescaped('''\n//<![CDATA[\n
          $(function() {
            $("body").children().remove();
            $("body").append("<div id='chart'/>");
            $("body").append("<table id='data'><thead><tr><th/></tr><tbody/></table>");

            var tableBody = $("table#data tbody");

            var endpoints = resultData.endpoints;

            var chartData = [];
            var endpointNames = [];
            var versions = [];
            var labels = [];

            $.each(endpoints, function(name, data) {
              endpointNames.push(name);
              $.each(data.results, function(version, data) {
                if ($.inArray(version, versions) < 0) {
                  versions.push(version);
                  chartData.push([]);
                  labels.push({label: version});
                  $("<tr class='version " + version + "'>").appendTo(tableBody).append("<th>" + version + "</th>");
                }
              });
            });

            var maxTime = 0;
            $.each(endpoints, function(endpoint, data) {
              var results = data.results;

              $("thead tr").append("<th>" + endpoint + "</td>");

              $.each(versions, function(index, version) {
                var num = 0;
                if (results.hasOwnProperty(version)) {
                  num = results[version].msPerRequest;
                }

                $("tbody tr.version." + version).append("<td>" + num + "</td>");

                maxTime = Math.max(maxTime, num);
                chartData[index].push(num);
              });
            });

            var width = (56 + (endpointNames.length * 231) + 2 + 10) + "px";
            $("#chart").css({width: width});
            $("#data").css({width: width});

            var scaleIncrement = 0.25;

            $.jqplot('chart', chartData, {
              seriesDefaults: {
                renderer: $.jqplot.BarRenderer,
                pointLabels: {
                  show: true,
                  location: 'n',
                  edgeTolerance: -15
                },
                rendererOptions: {
                  fillToZero: true
                }
              },
              axesDefaults: {
              },
              series: labels,
              legend: {
                show: true,
                location: 'nw',
              },
              axes: {
                xaxis: {
                  show: true,
                  renderer: $.jqplot.CategoryAxisRenderer,
                  ticks: endpointNames
                },
                yaxis: {
                  pad: 1.2,
                  min: 0,
                  max: (Math.ceil(maxTime * (1 / scaleIncrement)) * scaleIncrement),
                  tickOptions: {
                    formatString: '%.5f\'
                  }
                }
              },
            });
          });
        //]]>\n''')
      }
      style {
        mkp.yieldUnescaped("""
          table#data {
            margin-top: 2em;
            border-spacing: 0;
            border-collapse: collapse;
            font-family: monospace;
          }
          table#data td, table#data th {
            padding: 5px 0;
            border: 1px solid black;
          }
          table#data th {
            background-color: lightgrey;
          }
          table#data tbody td {
            width: 220px;
            padding-left: 10px;
          }
          table#data tbody tr.version th {
            width: 56px;
          }
        """)
      }
    }
  }


}
