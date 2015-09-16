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

package ratpack.handling.internal;

import ratpack.handling.RequestId;

public class DefaultRequestId implements RequestId {

  private final CharSequence id;

  public DefaultRequestId(CharSequence id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DefaultRequestId that = (DefaultRequestId) o;

    return (id != null && id.equals(that.id)) || (id == null && that.id == null);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @SuppressWarnings("NullableProblems")
  @Override
  public String toString() {
    return id.toString();
  }

  @Override
  public int length() {
    return id.length();
  }

  @Override
  public char charAt(int index) {
    return id.charAt(index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return id.subSequence(start, end);
  }
}
