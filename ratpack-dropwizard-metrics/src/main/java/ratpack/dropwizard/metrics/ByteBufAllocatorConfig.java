/*
 * Copyright 2018 the original author or authors.
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

public class ByteBufAllocatorConfig {
  private boolean enabled = true;
  private boolean detailed = true;

  /* The flag whether byte buf allocator metric set should be initialized.
   * @return the flag
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Set the flag whether byte buf allocator metric set should be initialized.
   *
   * @param enabled True if metrics set should be initialzed. False otherwise
   * @return this
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /* The flag whether byte buf allocator metric set should be report detailed metrics.
   * @return the flag
   */
  public boolean isDetailed() {
    return detailed;
  }

  /**
   * Set the flag whether byte buf allocator metric set should be report detailed metrics.
   *
   * @param detailed True if metrics set should be report detailed metrics. False otherwise
   * @return this
   */
  public void setDetailed(boolean detailed) {
    this.detailed = detailed;
  }
}
