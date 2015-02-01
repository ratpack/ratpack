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

package ratpack.site;

import ratpack.codahale.metrics.CodaHaleMetricsModule;
import ratpack.config.ConfigData;
import ratpack.file.FileSystemBinding;
import ratpack.file.internal.DefaultFileSystemBinding;
import ratpack.func.NoArgAction;
import ratpack.func.Pair;
import ratpack.groovy.template.MarkupTemplateModule;
import ratpack.groovy.template.TextTemplateModule;
import ratpack.guice.Guice;
import ratpack.jackson.JacksonModule;
import ratpack.newrelic.NewRelicModule;
import ratpack.rx.RxRatpack;
import ratpack.server.NoBaseDirException;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;
import ratpack.server.internal.DelegatingServerConfig;
import ratpack.site.github.GitHubApi;
import ratpack.site.github.GitHubData;
import ratpack.site.github.RatpackVersion;
import ratpack.site.github.RatpackVersions;

import java.util.Collections;
import java.util.List;

import static ratpack.groovy.Groovy.groovyMarkupTemplate;
import static ratpack.registry.Registries.just;

public class SiteMain {
  public static void main(String... args) throws Exception {
    RatpackServer.start(b -> {
        RxRatpack.initialize();
        ConfigData config = ConfigData.of(c -> c.env().sysProps());
        ServerConfig innerServerConfig = ServerConfig.findBaseDirProps().build();

        // This is needed because the config loading stuff doesn't quite handle portable base dir
        ServerConfig serverConfig = new DelegatingServerConfig(config.getServerConfig()) {
          @Override
          public FileSystemBinding getBaseDir() throws NoBaseDirException {
            return innerServerConfig.getBaseDir();
          }

          @Override
          public boolean isHasBaseDir() {
            return true;
          }

          @Override
          public List<String> getIndexFiles() {
            return Collections.singletonList("index.html");
          }
        };

        return b
          .config(serverConfig)
          .registry(
            Guice.registry(s -> s
                .add(JacksonModule.class)
                .add(NewRelicModule.class)
                .add(new CodaHaleMetricsModule(), c -> c.enable(true))
                .addConfig(SiteModule.class, config.get("/github", SiteModule.GitHubConfig.class))
                .add(MarkupTemplateModule.class, conf -> {
                  conf.setAutoNewLine(true);
                  conf.setUseDoubleQuotes(true);
                  conf.setAutoIndent(true);
                })
                .add(TextTemplateModule.class, conf ->
                    conf.setStaticallyCompile(true)
                )
            )
          )
          .handlers(c -> {

            int longCache = 60 * 60 * 24 * 365;
            int shortCache = 60 * 10; // ten mins

            c
              .handler(ctx -> {
                //noinspection ConstantConditions
                if (ctx.getRequest().getHeaders().get("host").endsWith("ratpack-framework.org")) {
                  ctx.redirect(301, "http://www.ratpack.io");
                  return;
                }

                if (ctx.getRequest().getPath().isEmpty() || ctx.getRequest().getPath().equals("index.html")) {
                  ctx.getResponse().getHeaders().set("X-UA-Compatible", "IE=edge,chrome=1");
                }

                ctx.next();
              })

              .prefix("assets", assets -> assets
                  .handler(ctx -> {
                    int cacheFor = ctx.getRequest().getQuery().isEmpty() ? longCache : shortCache;
                    ctx.getResponse().getHeaders().add("Cache-Control", "max-age=" + cacheFor + ", public");
                    ctx.next();
                  })
                  .assets("assets")
              )

              .get("index.html", ctx -> {
                ctx.redirect(301, "/");
              })

              .get(ctx -> ctx.render(groovyMarkupTemplate("index.gtpl")))

              .handler("reset", ctx -> {
                GitHubApi gitHubApi = ctx.get(GitHubApi.class);
                ctx.byMethod(methods -> {
                  NoArgAction impl = () -> {
                    gitHubApi.invalidateCache();
                    ctx.render("ok");
                  };
                  if (ctx.getServerConfig().isDevelopment()) {
                    methods.get(impl);
                  }
                  methods.post(impl);
                });
              })

              .prefix("versions", v -> v
                  .get(ctx ->
                      ctx.render(
                        ctx.get(RatpackVersions.class).getAll()
                          .map(all -> groovyMarkupTemplate("versions.gtpl", m -> m.put("versions", all)))
                      )
                  )
                  .get(":version", ctx ->
                      ctx.render(
                        ctx.get(RatpackVersions.class).getAll()
                          .map(all -> all.version(ctx.getAllPathTokens().get("version")))
                          .onNull(() -> ctx.clientError(404))
                          .flatMap(version -> ctx.get(GitHubData.class).closed(version).map(i -> Pair.of(version, i)))
                          .map(p -> groovyMarkupTemplate("version.gtpl", m -> m.put("version", p.left).put("issues", p.right)))
                      )
                  )
              )

              .prefix("manual", c1 -> c1
                  .fileSystem("manual", c2 -> c2
                      .get(ctx -> ctx.redirect(301, "manual/current"))
                      .prefix(":label", c3 -> c3
                          .handler(ctx -> {
                            String label = ctx.getPathTokens().get("label");

                            ctx.get(RatpackVersions.class).getAll().then(all -> {
                              if (label.equals("current") || all.isReleased(label)) {
                                ctx.getResponse().getHeaders().add("Cache-Control", "max-age=" + longCache + ", public");
                              } else if (label.equals("snapshot") || all.isUnreleased(label)) {
                                ctx.getResponse().getHeaders().add("Cache-Control", "max-age=" + shortCache + ", public");
                              }

                              RatpackVersion version;
                              switch (label) {
                                case "current":
                                  version = all.getCurrent();
                                  break;
                                case "snapshot":
                                  version = all.getSnapshot();
                                  break;
                                default:
                                  version = all.version(label);
                                  if (version == null) {
                                    ctx.clientError(404);
                                    return;
                                  }
                                  break;
                              }

                              ctx.next(just(new DefaultFileSystemBinding(ctx.file(version.getVersion()))));
                            });
                          })
                          .assets("")
                      )
                  )

              )

              .assets("public");
          });
      }
    );
  }
}
