/*
 * Copyright 2016 the original author or authors.
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

package ratpack.config.internal.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ratpack.config.ConfigSource;
import ratpack.file.FileSystemBinding;

public class ObjectConfigSource implements ConfigSource {

  private final String path;
  private final Object object;

  public ObjectConfigSource(String path, Object object) {
    this.path = path;
    this.object = object;
  }

  @Override
  public ObjectNode loadConfigData(ObjectMapper mapper,
                                   FileSystemBinding fileSystemBinding) throws Exception {
    ObjectNode node = mapper.createObjectNode();
    node.set(path, mapper.valueToTree(object));
    return node;
  }
}
