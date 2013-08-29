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

package org.ratpackframework.groovy.test.remote;

import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovyx.remote.transport.http.HttpTransport;
import org.ratpackframework.test.ApplicationUnderTest;

import static org.ratpackframework.remote.RemoteControlModule.DEFAULT_REMOTE_CONTROL_PATH;

public class RemoteControl extends groovyx.remote.client.RemoteControl {

  public RemoteControl(ApplicationUnderTest application, String path) {
    super(new HttpTransport(application.getAddress() + "/" + path));
  }

  public RemoteControl(ApplicationUnderTest application) {
    this(application, DEFAULT_REMOTE_CONTROL_PATH);
  }

  @Override
  public Object invokeMethod(String name, Object args) {
    return getMetaClass().invokeMethod(this, name, args);
  }

  @Override
  public Object getProperty(String propertyName) {
    return getMetaClass().getProperty(this, propertyName);
  }

  @Override
  public void setProperty(String propertyName, Object newValue) {
    getMetaClass().setProperty(this, propertyName, newValue);
  }

  @Override
  public MetaClass getMetaClass() {
    return GroovySystem.getMetaClassRegistry().getMetaClass(getClass());
  }

  @Override
  public void setMetaClass(MetaClass metaClass) {
    GroovySystem.getMetaClassRegistry().setMetaClass(getClass(), metaClass);
  }
}
