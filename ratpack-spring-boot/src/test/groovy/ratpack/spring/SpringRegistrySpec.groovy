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

package ratpack.spring

import com.google.common.reflect.TypeToken
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.context.support.StaticApplicationContext
import ratpack.func.Action
import ratpack.registry.Registry
import ratpack.registry.RegistrySpec
import ratpack.test.internal.registry.RegistryContractSpec

import java.util.function.Supplier

class SpringRegistrySpec extends RegistryContractSpec {

  @Override
  Registry build(Action<? super RegistrySpec> action) {
    def appContext = new StaticApplicationContext()
    def beanFactory = appContext.getBeanFactory()
    def nameCounter = 0

    def spec = new RegistrySpec() {
      @Override
      def <O> RegistrySpec addLazy(TypeToken<O> type, Supplier<? extends O> supplier) {
        def factoryName = "bean-factory-$nameCounter"
        beanFactory.registerSingleton(factoryName, new FactoryBean() {
          @Override
          Object getObject() throws Exception {
            return supplier.get()
          }

          @Override
          Class<?> getObjectType() {
            return type.getRawType()
          }

          @Override
          boolean isSingleton() {
            return true
          }
        })
        def builder = BeanDefinitionBuilder.genericBeanDefinition()
        builder.getRawBeanDefinition().setFactoryBeanName(factoryName)
        appContext.registerBeanDefinition("bean-${nameCounter++}", builder.getBeanDefinition())
        this
      }
    }

    action.execute(spec)
    Spring.spring(appContext)
  }

}
