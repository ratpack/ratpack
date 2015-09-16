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

package ratpack.dropwizard.metrics.internal;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

/**
 * A filter that uses a regular expression for filtering.
 */
public class RegexMetricFilter implements MetricFilter {
  private static final String DEFAULT_INCLUDE_REGEX = ".*";
  private static final String DEFAULT_EXCLUDE_REGEX = "";

  private final String includeRegex;
  private final String excludeRegex;

  public RegexMetricFilter(String includeRegex, String excludeRegex) {
    this.includeRegex = includeRegex != null ? includeRegex : DEFAULT_INCLUDE_REGEX;
    this.excludeRegex = excludeRegex != null ? excludeRegex : DEFAULT_EXCLUDE_REGEX;
  }

  @Override
  public boolean matches(String name, Metric metric) {
    return name.matches(includeRegex) && !name.matches(excludeRegex);
  }
}
