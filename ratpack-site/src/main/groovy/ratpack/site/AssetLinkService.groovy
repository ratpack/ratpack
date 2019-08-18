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

package ratpack.site

import asset.pipeline.AssetPipelineConfigHolder
import ratpack.file.internal.FileSystemChecksumService

class AssetLinkService {

  private final FileSystemChecksumService checksumService

  AssetLinkService(FileSystemChecksumService checksumService) {
    this.checksumService = checksumService
  }

  String getAt(String path) {
  	final Properties manifest = AssetPipelineConfigHolder.manifest
    String manifestPath = path.startsWith("/") ? path.substring(1) : path
    if (manifest) {
    	manifestPath = manifest.getProperty(manifestPath, manifestPath)
    }
    return "/assets/${manifestPath}"
  }

}
