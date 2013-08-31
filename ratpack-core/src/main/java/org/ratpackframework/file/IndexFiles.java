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

package org.ratpackframework.file;

import java.util.List;

/**
 * Represents the index files to try when an {@link org.ratpackframework.file.internal.AssetHandler}
 * <code>Request</code> is for a directory.
 * <p>
 * Index files are applied at 3 levels and in this order:
 * <ol>
 *   <li>
 *     At the <code>Handler</code> level
 *     <pre>
 *      app {
 *        handlers {
 *          assets("public", "index.html")
 *        }
 *      }
 *     </pre>
 *   </li>
 *   <li>
 *     At the <code>Module</code> level
 *     <pre>
 *      app {
 *        modules {
 *          bind IndexFiles, new IndexFiles() {
 *            List<String> getFileNames() { ["index.html"] }
 *          }
 *        }
 *        handlers {
 *          assets("public")
 *        }
 *      }
 *     </pre>
 *   </li>
 *   <li>
 *     At the global level by setting the RatPack property <code>other.indexFiles</code>
 *     <p>
 *     The property is a comma separated list of file names <code>custom.xhtml,custom.html</code>
 *   </li>
 * </ol>
 *
 * @see org.ratpackframework.file.internal.AssetHandler
 * @see org.ratpackframework.handling.Handlers#assets(String, String...)
 */
public interface IndexFiles {

  List<String> getFileNames();

}
