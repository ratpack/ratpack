/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.spring.config;

import java.util.Collections;
import java.util.List;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.google.inject.Module;

import ratpack.func.Function;
import ratpack.guice.Guice;
import ratpack.registry.Registry;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;
import ratpack.spring.Spring;
import ratpack.spring.config.internal.ChainConfigurers;

/**
 * @author Dave Syer
 *
 */
@Configuration
@Import(ChainConfigurers.class)
@EnableConfigurationProperties(RatpackProperties.class)
public class RatpackConfiguration implements CommandLineRunner {

  @Autowired
  private RatpackServer server;

  @Override
  public void run(String... args) throws Exception {
    server.start();
  }

  @PreDestroy
  public void stop() throws Exception {
    server.stop();
  }

  @Configuration
  protected static class ServerConfigConfiguration {

    @Autowired
    private RatpackProperties ratpack;

    @Autowired(required = false)
    private List<RatpackServerCustomizer> customizers = Collections.emptyList();

    @Bean
    @ConditionalOnMissingBean
    public ServerConfig ratpackServerConfig() throws Exception {
      ServerConfig.Builder serverConfigBuilder = ServerConfig.baseDir(ratpack.getBasepath())
          .address(ratpack.getAddress()).threads(ratpack.getMaxThreads());

      if (ratpack.getPort() != null) {
        serverConfigBuilder.port(ratpack.getPort());
      }

      for (RatpackServerCustomizer customizer : customizers) {
        customizer.getServerConfig().execute(serverConfigBuilder);
      }

      return serverConfigBuilder.build();
    }

  }

  @Configuration
  protected static class ServerConfiguration {

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private ChainConfigurers chainConfigurers;

    @Autowired(required = false)
    private List<RatpackServerCustomizer> customizers = Collections.emptyList();

    @Bean
    public RatpackServer ratpackServer(ApplicationContext context) throws Exception {
      return RatpackServer.of(ratpackServerSpec -> ratpackServerSpec.serverConfig(serverConfig)
          .registry(joinedRegistry(context)).handlers(chainConfigurers));
    }

    private Function<Registry, Registry> joinedRegistry(ApplicationContext context) throws Exception {
      return baseRegistry -> Spring.spring(context).join(Guice.registry(bindingSpec -> {
        context.getBeansOfType(Module.class).values().forEach(bindingSpec::module);
        for (RatpackServerCustomizer customizer : customizers) {
          customizer.getBindings().execute(bindingSpec);
        }
      }).apply(baseRegistry));
    }

  }

}
