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

package ratpack.session.clientside;

import java.io.Serializable;

public class SerializableTypes implements Serializable {

  public static class TypeA implements Serializable {
    private static final long serialVersionUID = -7232197649235323121L;

    Integer valueInt;
    Double valueDouble;

    public Integer getValueInt() {
      return valueInt;
    }

    public void setValueInt(Integer valueInt) {
      this.valueInt = valueInt;
    }

    public Double getValueDouble() {
      return valueDouble;
    }

    public void setValueDouble(Double valueDouble) {
      this.valueDouble = valueDouble;
    }
  }

  public static class TypeB implements Serializable {
    private static final long serialVersionUID = -125230090472909810L;

    String valueStr;
    TypeA typeA;

    public String getValueStr() {
      return valueStr;
    }

    public void setValueStr(String valueStr) {
      this.valueStr = valueStr;
    }

    public TypeA getTypeA() {
      return typeA;
    }

    public void setTypeA(TypeA typeA) {
      this.typeA = typeA;
    }
  }
}
