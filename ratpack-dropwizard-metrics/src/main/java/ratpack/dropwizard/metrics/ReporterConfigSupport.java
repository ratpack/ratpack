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
package ratpack.dropwizard.metrics;

/**
 * A common base for reporter config classes.
 *
 * @param <T> self type
 * @since 1.4
 */
public abstract class ReporterConfigSupport<T extends ReporterConfigSupport<T>> {
  private String includeFilter;
  private String excludeFilter;
  private boolean enabled = true;

  /**
   * The include metric filter expression of the reporter.
   *
   * @return the include filter
   */
  public String getIncludeFilter() {
    return this.includeFilter;
  }

  /**
   * Set the include metric filter of the reporter.
   *
   * @param includeFilter the regular expression to match on.
   * @return {@code this}
   */
  @SuppressWarnings("unchecked")
  public T includeFilter(String includeFilter) {
    this.includeFilter = includeFilter;
    return (T)this;
  }

  /**
   * The exclude metric filter expression of the reporter.
   *
   * @return the exclude filter
   */
  public String getExcludeFilter() {
    return this.excludeFilter;
  }

  /**
   * Set the exclude metric filter expression of the reporter.
   *
   * @param excludeFilter the regular expression to match on.
   * @return {@code this}
   */
  @SuppressWarnings("unchecked")
  public T excludeFilter(String excludeFilter) {
    this.excludeFilter = excludeFilter;
    return (T)this;
  }

  /**
   * The state of the publisher.
   * <p>
   * True by default if the reporter configuration is specified.
   *
   * @return the state of the publisher
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Set the state of the publisher.
   * @param enabled True if metrics are published to this publisher. False otherwise
   * @return this
   */
  @SuppressWarnings("unchecked")
  public T enable(boolean enabled) {
    this.enabled = enabled;
    return (T)this;
  }

}
