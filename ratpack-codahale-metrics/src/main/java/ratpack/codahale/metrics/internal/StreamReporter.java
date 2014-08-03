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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.launch.LaunchConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ScheduledReporter} that outputs measurements to a {@link MetricsBroadcaster} in JSON format.
 * <p>
 * The reporting interval, in seconds, can be specified by setting a configuration property with the name <code>metrics.scheduledreporter.interval</code>.
 * If no interval is specified a default interval will be used which is defined by {@link #DEFAULT_INTERVAL}.
 */
public class StreamReporter extends ScheduledReporter {

  private final static Logger LOGGER = LoggerFactory.getLogger(StreamReporter.class);
  /**
   * The default reporting interval.
   */
  private final static String DEFAULT_INTERVAL = "30";
  private final MetricsBroadcaster metricsBroadcaster;
  private final Clock clock = Clock.defaultClock();
  private final JsonFactory factory = new JsonFactory();

  @Inject
  public StreamReporter(MetricRegistry registry, MetricsBroadcaster metricsBroadcaster, LaunchConfig launchConfig) {
    super(registry, "websocket-reporter", MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
    this.metricsBroadcaster = metricsBroadcaster;
    String interval = launchConfig.getOther("metrics.scheduledreporter.interval", DEFAULT_INTERVAL);
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
      writeGauges(json, gauges);
      writeMeters(json, meters);
      writeCounters(json, counters);
      writeHistograms(json, histograms);
      json.writeEndObject();

      json.flush();
      json.close();

      metricsBroadcaster.broadcast(out.toString());
    } catch (IOException e) {
      LOGGER.warn("Exception encountered while reporting metrics: " + e.getLocalizedMessage());
    }
  }

  private void writeHistograms(JsonGenerator json, SortedMap<String, Histogram> histograms) throws IOException {
    json.writeArrayFieldStart("histograms");
    for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
      Histogram histogram = entry.getValue();

      json.writeStartObject();
      json.writeStringField("name", entry.getKey());
      json.writeNumberField("count", histogram.getCount());

      Snapshot snapshot = histogram.getSnapshot();
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

  private void writeCounters(JsonGenerator json, SortedMap<String, Counter> counters) throws IOException {
    json.writeArrayFieldStart("counters");
    for (Map.Entry<String, Counter> entry : counters.entrySet()) {
      Counter counter = entry.getValue();

      json.writeStartObject();
      json.writeStringField("name", entry.getKey());
      json.writeNumberField("count", counter.getCount());
      json.writeEndObject();
    }
    json.writeEndArray();
  }

  private void writeMeters(JsonGenerator json, SortedMap<String, Meter> meters) throws IOException {
    json.writeArrayFieldStart("meters");
    for (Map.Entry<String, Meter> entry : meters.entrySet()) {
      Meter meter = entry.getValue();

      json.writeStartObject();
      json.writeStringField("name", entry.getKey());
      json.writeNumberField("count", meter.getCount());
      json.writeNumberField("meanRate", convertRate(meter.getMeanRate()));
      json.writeNumberField("oneMinuteRate", convertRate(meter.getOneMinuteRate()));
      json.writeNumberField("fiveMinuteRate", convertRate(meter.getFiveMinuteRate()));
      json.writeNumberField("fifteenMinuteRate", convertRate(meter.getFifteenMinuteRate()));
      json.writeEndObject();
    }
    json.writeEndArray();
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

  @SuppressWarnings("rawtypes")
  private void writeGauges(JsonGenerator json, SortedMap<String, Gauge> gauges) throws IOException {
    json.writeArrayFieldStart("gauges");
    for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
      Gauge gauge = entry.getValue();

      json.writeStartObject();
      json.writeStringField("name", entry.getKey());
      try {
        json.writeFieldName("value");
        json.writeObject(gauge.getValue());
      } catch (Exception e) {
        LOGGER.debug("Exception encountered while reporting [" + entry.getKey() + "]: " + e.getLocalizedMessage());
        json.writeNull();
      }
      json.writeEndObject();

    }
    json.writeEndArray();
  }

}
