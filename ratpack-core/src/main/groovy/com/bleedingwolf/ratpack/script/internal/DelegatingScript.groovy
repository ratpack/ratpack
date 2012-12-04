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

package com.bleedingwolf.ratpack.script.internal

public abstract class DelegatingScript extends Script {
  private volatile GroovyObject $delegate;

  public void $setDelegate(GroovyObject delegate) {
    this.$delegate = delegate;
  }

  @Override
  public Object getProperty(String property) {
    try {
      return $delegate.getProperty(property)
    } catch (MissingPropertyException e) {
      return super.getProperty(property)
    }
  }

  @Override
  public void setProperty(String property, Object newValue) {
    try {
      $delegate.setProperty(property, newValue)
    } catch (MissingPropertyException e) {
      super.setProperty(property, newValue)
    }
  }

  @Override
  public Object invokeMethod(String name, Object args) {
    try {
      return $delegate.invokeMethod(name, args)
    } catch (MissingMethodException e) {
      return super.invokeMethod(name, args)
    }
  }
}
