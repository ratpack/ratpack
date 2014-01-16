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

import ratpack.api.Nullable;
import ratpack.util.MultiValueMap;

import java.lang.UnsupportedOperationException;
import java.util.List;

/**
 * An uploaded form.
 * <p>
 * The form is modelled as a {@link ratpack.util.MultiValueMap}, with extra methods for dealing with file uploads.
 * That is, uploaded files are not visible via the methods provided by {@link ratpack.util.MultiValueMap}.
 * <p>
 * All instances of this type are <b>immutable</b>.
 * Calling any mutative method of {@link ratpack.util.MultiValueMap} will result in an {@link UnsupportedOperationException}.
 *
 * @see Forms#form()
 */
public interface Form extends MultiValueMap<String, String> {

  /**
   * Return the first uploaded file with the given name.
   *
   * @param name The name of the uploaded file in the form
   * @return The uploaded file, or {@code null} if no file was uploaded by that name
   */
  @Nullable
  UploadedFile file(String name);

  /**
   * Return all of the uploaded files with the given name.
   *
   * @param name The name of the uploaded files in the form
   * @return The uploaded files, or an empty list if no files were uploaded by that name
   */
  List<UploadedFile> files(String name);

  /**
   * Returns all of the uploaded files.
   *
   * @return all of the uploaded files.
   */
  MultiValueMap<String, UploadedFile> files();

}
