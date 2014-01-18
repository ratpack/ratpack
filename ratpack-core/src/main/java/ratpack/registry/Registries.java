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

package ratpack.registry;

import ratpack.util.Factory;

public abstract class Registries {

  private Registries() {
  }

  public static <T> Registry registry(Class<? super T> publicType, T implementation) {
    return RegistryBuilder.builder().add(publicType, implementation).build();
  }

  public static <T> Registry registry(Class<T> publicType, Factory<? extends T> factory) {
    return RegistryBuilder.builder().add(publicType, factory).build();
  }

  public static Registry registry(Object object) {
    return RegistryBuilder.builder().add(object).build();
  }

}
