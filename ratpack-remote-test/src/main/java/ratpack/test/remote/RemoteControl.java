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

package ratpack.test.remote;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovyx.remote.Result;
import groovyx.remote.transport.http.HttpTransport;
import ratpack.remote.CommandDelegate;
import ratpack.test.ApplicationUnderTest;

import java.util.Map;

import static ratpack.remote.RemoteControlModule.DEFAULT_REMOTE_CONTROL_PATH;

public class RemoteControl extends groovyx.remote.client.RemoteControl {

  public RemoteControl(ApplicationUnderTest application, String path) {
    super(new HttpTransport(application.getAddress() + path));
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

  @Override
  public Object exec(@DelegatesTo(value = CommandDelegate.class, strategy = Closure.DELEGATE_FIRST) Closure... commands) {
    return super.exec(commands);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Object exec(Map params, @DelegatesTo(value = CommandDelegate.class, strategy = Closure.DELEGATE_FIRST) Closure... commands) {
    return super.exec(params, commands);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Object call(Map params, @DelegatesTo(value = CommandDelegate.class, strategy = Closure.DELEGATE_FIRST) Closure... commands) {
    return super.call(params, commands);
  }

  @Override
  public Object call(@DelegatesTo(value = CommandDelegate.class, strategy = Closure.DELEGATE_FIRST) Closure... commands) {
    return super.call(commands);
  }

  @Override
  protected Object processResult(Result result) {
    return super.processResult(result);
  }
}
