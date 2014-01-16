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

package ratpack.form;

import io.netty.buffer.ByteBuf;
import ratpack.api.Nullable;
import ratpack.http.MediaType;

/**
 * A file that was uploaded via a form.
 *
 * @see Form
 */
public interface UploadedFile {

  /**
   * The advertised content type of the form.
   *
   * @return The advertised content type of the form, or {@code null} if no content type was provided.
   */
  @Nullable
  MediaType getContentType();

  /**
   * The name given for the file.
   *
   * @return The name given for the file, or {@code null} if no name was provided.
   */
  @Nullable
  String getFileName();

  /**
   * The content of the uploaded file.
   *
   * @return The content of the uploaded file.
   */
  ByteBuf getBuffer();

  /**
   * The content of the uploaded file.
   *
   * @return The content of the uploaded file.
   */
  byte[] getBytes();

  /**
   * The content of the uploaded file.
   * <p>
   * The content will be interpreted with the encoding specified by {@link #getContentType()}.
   * If no encoding was explicitly specified as part of the content type, or if no content type was provided, {@code UTF-8} will be used.
   *
   * @return The content of the uploaded file.
   */
  String getText();

}
