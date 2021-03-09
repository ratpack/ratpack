/*
 * Copyright 2021 the original author or authors.
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

package ratpack.session.internal;

import com.google.common.collect.Sets;
import ratpack.session.AllowedSessionType;
import ratpack.session.SessionTypeFilter;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.util.List;
import java.util.Set;

public class DefaultSessionTypeFilter implements SessionTypeFilter {

  public static final DefaultSessionTypeFilter INSTANCE = new DefaultSessionTypeFilter();
  private static final Set<Package> ALLOWED_PACKAGES = Sets.newIdentityHashSet();
  private static final Set<Class<?>> ALLOWED_CLASSES = Sets.newIdentityHashSet();

  static {
    ALLOWED_PACKAGES.add(String.class.getPackage()); // java.lang
    ALLOWED_PACKAGES.add(List.class.getPackage()); // java.util

    ALLOWED_CLASSES.add(NotSerializableException.class);
    ALLOWED_CLASSES.add(ObjectStreamException.class);
    ALLOWED_CLASSES.add(IOException.class);
  }

  @Override
  public boolean allow(Class<?> type) {
    return hasAnnotation(type) || isAllowedJdkType(type) || type.isArray();
  }

  private boolean isAllowedJdkType(Class<?> type) {
    return type.getClassLoader() == String.class.getClassLoader()
      && (ALLOWED_PACKAGES.contains(type.getPackage()) || ALLOWED_CLASSES.contains(type));
  }

  private static boolean hasAnnotation(Class<?> type) {
    return type.getAnnotation(AllowedSessionType.class) != null;
  }

}
