package org.ratpackframework.assets.internal;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.ratpackframework.assets.StaticAssetsConfig;
import org.ratpackframework.bootstrap.internal.RootModule;
import org.ratpackframework.Action;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.CompositeRouter;
import org.ratpackframework.routing.Routed;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.ratpackframework.bootstrap.internal.RootModule.MAIN_STATIC_ASSET_HTTP_HANDLER;

public class StaticAssetsModule extends AbstractModule {

  public static final String MAIN_STATIC_ASSET_ROUTING_PIPELINE = "mainStaticAssetRoutingPipeline";
  public static final String TARGET_FILE_STATIC_ASSET_HANDLER = "targetFileStaticAssetHandler";
  public static final String DIRECTORY_STATIC_ASSET_HANDLER = "directoryStaticAssetHandler";
  public static final String FILE_STATIC_ASSET_HANDLER = "fileStaticAssetHandler";
  public static final String INDEX_FILES = "indexFiles";
  public static final String STATIC_ASSET_DIR = "staticAssetDir";

  private final File dir;
  private final List<String> indexFiles;

  public StaticAssetsModule(StaticAssetsConfig config) {
    this(config.getDirectory(), config.getIndexFiles());
  }

  public StaticAssetsModule(File dir, List<String> indexFiles) {
    this.dir = dir;
    this.indexFiles = new ArrayList<>(indexFiles);
  }

  @Override
  protected void configure() {
    bind(RootModule.HTTP_HANDLER).annotatedWith(Names.named(MAIN_STATIC_ASSET_HTTP_HANDLER)).toProvider(StaticAssetRouterProvider.class);
    bind(RootModule.HTTP_HANDLER_PIPELINE).annotatedWith(Names.named(MAIN_STATIC_ASSET_ROUTING_PIPELINE)).toProvider(StaticAssetRoutingPipelineProvider.class);
    bind(RootModule.HTTP_HANDLER).annotatedWith(Names.named(TARGET_FILE_STATIC_ASSET_HANDLER)).to(TargetFileStaticAssetRequestHandler.class);
    bind(RootModule.HTTP_HANDLER).annotatedWith(Names.named(DIRECTORY_STATIC_ASSET_HANDLER)).to(DirectoryStaticAssetRequestHandler.class);
    bind(RootModule.HTTP_HANDLER).annotatedWith(Names.named(FILE_STATIC_ASSET_HANDLER)).to(FileStaticAssetRequestHandler.class);
    bind(RootModule.STRING_LIST).annotatedWith(Names.named(INDEX_FILES)).toInstance(indexFiles);
    bind(File.class).annotatedWith(Names.named(STATIC_ASSET_DIR)).toInstance(dir);
  }

  public static class StaticAssetRouterProvider implements Provider<Action<Routed<HttpExchange>>> {
    @Inject
    @Named(MAIN_STATIC_ASSET_ROUTING_PIPELINE)
    List<Action<Routed<HttpExchange>>> pipeline;

    @Override
    public Action<Routed<HttpExchange>> get() {
      return new CompositeRouter<>(pipeline);
    }
  }

  public static class StaticAssetRoutingPipelineProvider implements Provider<List<Action<Routed<HttpExchange>>>> {
    @Inject
    @Named(TARGET_FILE_STATIC_ASSET_HANDLER)
    Action<Routed<HttpExchange>> targetFile;

    @Inject
    @Named(DIRECTORY_STATIC_ASSET_HANDLER)
    Action<Routed<HttpExchange>> directory;

    @Inject
    @Named(FILE_STATIC_ASSET_HANDLER)
    Action<Routed<HttpExchange>> file;

    @Override
    public List<Action<Routed<HttpExchange>>> get() {
      return Arrays.asList(targetFile, directory, file);
    }
  }

}
