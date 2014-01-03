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

package ratpack.remote.internal;

import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovyx.remote.CommandChain;
import groovyx.remote.Result;
import groovyx.remote.server.CommandChainInvoker;
import groovyx.remote.server.ContextFactory;
import groovyx.remote.server.Receiver;
import ratpack.registry.RegistrySpec;

public class CustomReceiver extends Receiver {

  public CustomReceiver(ContextFactory contextFactory) {
    super(contextFactory);
  }

  @Override
  protected Object createInvoker(ClassLoader classLoader, CommandChain commandChain) {
    return new CommandChainInvoker(classLoader, commandChain) {
      @Override
      public Result invokeAgainst(Object delegate) {
        Result result = super.invokeAgainst(delegate);
        if (result.getUnserializable() instanceof RegistrySpec) {
          return (Result) Result.forNull();
        } else {
          return result;
        }
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

    };
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
