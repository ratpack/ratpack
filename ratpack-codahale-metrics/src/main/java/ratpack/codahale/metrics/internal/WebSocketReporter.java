/*
 * Copyright 2013 the original author or authors.
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

package ratpack.codahale.metrics.internal;

import com.codahale.metrics.*;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.inject.Inject;
import ratpack.launch.LaunchConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebSocketReporter extends ScheduledReporter {

  private final static Logger LOGGER = Logger.getLogger(WebSocketReporter.class.getName());
  private final MetricsBroadcaster metricsBroadcaster;
  private final Clock clock = Clock.defaultClock();
  private final JsonFactory factory = new JsonFactory();

  @Inject
  public WebSocketReporter(MetricRegistry registry, MetricsBroadcaster metricsBroadcaster, LaunchConfig launchConfig) {
    super(registry, "websocket-reporter", MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
    this.metricsBroadcaster = metricsBroadcaster;
    String interval = launchConfig.getOther("metrics.scheduledreporter.interval", "30");
    this.start(Long.valueOf(interval), TimeUnit.SECONDS);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void report(SortedMap<String, Gauge> gauges,
                              SortedMap<String, Counter> counters,
                              SortedMap<String, Histogram> histograms,
                              SortedMap<String, Meter> meters,
                              SortedMap<String, Timer> timers) {
    try {
      OutputStream out = new ByteArrayOutputStream();
      JsonGenerator json = factory.createGenerator(out);

      json.writeStartObject();
      json.writeNumberField("timestamp", clock.getTime());
      writeTimers(json, timers);
      json.writeEndObject();

      json.flush();
      json.close();

      metricsBroadcaster.broadcast(out.toString());
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Exception encountered while reporting metrics: " + e.getLocalizedMessage());
    }
  }

  private void writeTimers(JsonGenerator json, SortedMap<String, Timer> timers) throws IOException {
    json.writeArrayFieldStart("timers");
    for (Map.Entry<String, Timer> entry : timers.entrySet()) {
      Timer timer = entry.getValue();

      json.writeStartObject();
      json.writeStringField("name", entry.getKey());
      json.writeNumberField("count", timer.getCount());
      json.writeNumberField("meanRate", convertRate(timer.getMeanRate()));
      json.writeNumberField("oneMinuteRate", convertRate(timer.getOneMinuteRate()));
      json.writeNumberField("fiveMinuteRate", convertRate(timer.getFiveMinuteRate()));
      json.writeNumberField("fifteenMinuteRate", convertRate(timer.getFifteenMinuteRate()));

      Snapshot snapshot = timer.getSnapshot();
      json.writeNumberField("min", convertDuration(snapshot.getMin()));
      json.writeNumberField("max", convertDuration(snapshot.getMax()));
      json.writeNumberField("mean", convertDuration(snapshot.getMean()));
      json.writeNumberField("stdDev", convertDuration(snapshot.getStdDev()));
      json.writeNumberField("50thPercentile", convertDuration(snapshot.getMedian()));
      json.writeNumberField("75thPercentile", convertDuration(snapshot.get75thPercentile()));
      json.writeNumberField("95thPercentile", convertDuration(snapshot.get95thPercentile()));
      json.writeNumberField("98thPercentile", convertDuration(snapshot.get98thPercentile()));
      json.writeNumberField("99thPercentile", convertDuration(snapshot.get99thPercentile()));
      json.writeNumberField("999thPercentile", convertDuration(snapshot.get999thPercentile()));
      json.writeEndObject();
    }
    json.writeEndArray();
  }

}

