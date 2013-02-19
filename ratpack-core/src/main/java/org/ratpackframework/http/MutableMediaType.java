/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.http;

import java.util.LinkedHashMap;
import java.util.Map;

public class MutableMediaType extends MediaType {

  public MutableMediaType(String headerValue) {
    super(headerValue);
  }

  public MutableMediaType() {
    super(null);
  }

  @Override
  protected Map<String, String> emptyMap() {
    return new LinkedHashMap<>();
  }

  /**
   * @see #getBase()
   */
  @Override
  public void setBase(String base) {
    super.setBase(base);
  }

  /**
   * Sets the base.
   *
   * @return this
   */
  public MutableMediaType base(String base) {
    setBase(base);
    return this;
  }

  @Override
  public Map<String, String> getParams() {
    return this.params; // don't call super, it returns an unmodifiable
  }

  /**
   * Adds the following params, replacing any duplicates.
   *
   * Keys and values will be toString()'d.
   *
   * @return this
   */
  public MutableMediaType params(Map<?, ?> params) {
    for (Map.Entry<?, ?> entry : params.entrySet()) {
      getParams().put(entry.getKey().toString(), entry.getValue().toString());
    }
    return this;
  }

  /**
   * Sets the charset param.
   *
   * @return this
   */
  public MutableMediaType charset(String charset) {
    getParams().put("charset", charset);
    return this;
  }

  /**
   * Calls {@code charset("utf-8")}.
   *
   * @return this
   */
  public MutableMediaType utf8() {
    return charset("utf-8");
  }

  /**
   * Calls {@code base(base).utf8()}.
   *
   * @return this
   */
  public MutableMediaType utf8(String base) {
    return base(base).utf8();
  }
}
