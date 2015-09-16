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

package ratpack.file;

/**
 * A registry for mime types.
 *
 * Every exchange has an instance of this type available via the service.
 * The default implementation uses the {@link javax.activation.MimetypesFileTypeMap} class.
 */
public interface MimeTypes {

  /**
   * Calculate the mime type for the given file.
   *
   * @param name The file name to calculate the mime type for.
   * @return The mime type for the file name.
   */
  String getContentType(String name);

}
