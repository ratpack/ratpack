# Static Assets

Ratpack provides a convenient [Action<Chain>](api/ratpack/handling/Chain.html) helper for serving static files relative to Ratpack's [FileSystemBinding](api/ratpack/file/FileSystemBinding.html). One can turn on serving of static files simply by adding the `files` helper to the chain.

```language-java assets
package my.app;

import ratpack.server.RatpackServer;

public class Main {
  public static void main(String... args) throws Exception {
    RatpackServer.start(server -> server
      .serverConfig(d -> d.baseDir(BaseDir.find()))
      .handlers(chain -> chain
        .files(f -> f.dir("assets"))
      )
    );
  }
}
```

The above example will make all files within the `src/ratpack/assets` (or `${baseDir}/assets`) available to be served at the root URL unless another handler is previously matched in the chain. **Note:** This method used to be named `assets` and has since changed for clarity.

The default `Handler` for serving static files can handle sending files with `ETag` headers based on modification dates as well as serving gzip compressed files if the client accepts the gzip encoding. However, the current implementation will use CPU overhead to compress the stream on the fly and will even compress already compressed formats such as `PNG` or `JPG` images. It is still recommended (as is with any web application) to use a CDN to better optimize the transmission of static content to the browser as well as cache static assets.

## Sending from Handlers

Ratpack provides a method on the [Response](api/ratpack/http/Response.html#sendFile-java.nio.file.Path-) object for sending files. Simply pass a `Path` reference via the `sendFile()` method.

## Asset-Pipeline

For applications that require more advanced handling of static assets. There is now a [ratpack-asset-pipeline](http://github.com/bertramdev/ratpack-asset-pipeline) module that can be used. This module provides options for bundling, transpiling, live reloading, minification, and digesting of static assets while still maintaining a more performant handler for serving assets.

### Installing Asset-Pipeline

It is important to first note that asset-pipeline requires the use of groovy and guice modules currently. Simply add both the gradle plugin and the ratpack module to your `build.gradle` file:

```language-groovy gradle
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath "io.ratpack:ratpack-gradle:@ratpack-version@"
    classpath "com.bertramlabs.plugins:asset-pipeline-gradle:2.6.4"
    //Example additional LESS support
    //classpath "com.bertramlabs.plugins:less-asset-pipeline:2.6.4"
  }
}

apply plugin: "io.ratpack.ratpack-groovy"
apply plugin: "asset-pipeline"

dependencies {
    compile ratpack.dependency("guice")
    compile "com.bertramlabs.plugins:ratpack-asset-pipeline:2.6.4"
    //Example additional LESS support
    //provided "com.bertramlabs.plugins:less-asset-pipeline:2.6.4"
}
```

Now register the module in your application (`ratpack.groovy`):

```language-groovy
import asset.pipeline.ratpack.AssetPipelineModule

ratpack {
  bindings {
    ConfigData configData = ConfigData.of { it.sysProps().build() }
    moduleConfig(new AssetPipelineModule(), configData.get(AssetPipelineModule.Config))
  }
}
```

By default the module will register a handler to serve assets from the `/assets` prefixed url. Additionally it is possible to serve assets from another url prefix by adding the Handler to the chain.

```language-groovy
import asset.pipeline.ratpack.AssetPipelineModule
import asset.pipeline.ratpack.AssetPipelineHandler

ratpack {
  bindings {
    ConfigData configData = ConfigData.of { it.sysProps().build() }
    moduleConfig(new AssetPipelineModule(), configData.get(AssetPipelineModule.Config))
  }
  handlers {
  	    all(new AssetPipelineHandler())
  }
}
```

### Using Asset-Pipeline

By default the asset-pipeline will serve all assets from the `baseDir/assets/{}/**` folder (in many cases thats `src/ratpack/assets/javascripts` or `src/ratpack/assets/stylesheets`).

In the `development` environment this module will transpile and process files being served in realtime. It is also possible to combine many files using `Directives`:

```language-javascript application.js
//=require jquery
//=require_tree .
//=require_self

console.log("Hello World");
```

Now by simply requesting the file at `/assets/application.js` a combined file will be served containing jquery (if on resolver path), all files in the same directory, and finally the contents of the js file itself.

This module supports adding additional modules for features like [coffee-script](https://github.com/bertramdev/coffee-asset-pipeline), [LESS](https://github.com/bertramdev/less-asset-pipeline), [Handlebars](https://github.com/bertramdev/handlebars-asset-pipeline), [SASS](https://github.com/bertramdev/sass-asset-pipeline), and more.

Simply by adding `less-asset-pipeline` to both your buildscript dependencies as well as your runtime dependencies (provided if possible) you can reference less files in your static assets folders as if they were css files.


### Preparing for Production

During the build phase, asset-pipeline will compile all files in your `src/ratpack/assets` folder. All files will be given a digest md5 sum name suffix as well as a gzipped copy of the file. These will be stored along with a referencing `manifest` in your final distribution jar. When the asset-pipeline is running in production, no live transpiling is taking place. All files are served using the internal ratpack file sending capabilities. Files are also minified by default and relative urls in both css and html static files are recalculated to include digest file names that are matched within your assets folder. There are several configuration options for manipulating the behavior of how assets are packaged for production and this information can be found in the [asset-pipeline-core readme](https://github.com/bertramdev/asset-pipeline-core/blob/master/Readme.markdown).

There are a few improvements to using this module for serving assets vs. the standard `files` handler.
The asset-pipeline handler will cache file attributes from the file system upon the first request. This reduces the number of blocking i/o heavy calls involves when serving files. The asset-pipeline will also serve gzip files that have been compressed at build time rather than gzipping at runtime (Compressed image formats are not sent in a gzip encoding as the overhead is not worth the benefit).

Another series of improvements has to do with handling the browsers cached headers as well as proxy caching. Instead of relying on the files modified date `ETags` are generated based on the digest name of the file. A file can be requested by either its digest name or non digest name in order to better handle areas of the application that may not yet support digested file name requests.
