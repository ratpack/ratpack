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

package ratpack.file.internal;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import ratpack.file.MimeTypes;

import javax.activation.MimetypesFileTypeMap;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public class ActivationBackedMimeTypes implements MimeTypes {

  private static final MimetypesFileTypeMap MIME_TYPES_MAP = new MimetypesFileTypeMap();

  public static final MimeTypes INSTANCE = new ActivationBackedMimeTypes();

  private ActivationBackedMimeTypes() {
  }

  @Override
  public String getContentType(String name) {
    return MIME_TYPES_MAP.getContentType(name);
  }

  private static Set<String> extractKnownMimeTypes() {
    try {
      Function<Object, String> getMimeTypeFunction = new GetMimeTypeFunction();
      Field typeHashField = makeFieldAccessible(Class.forName("com.sun.activation.registries.MimeTypeFile"), "type_hash");
      Field mimeTypeFilesField = makeFieldAccessible(MimetypesFileTypeMap.class, "DB");
      Object mimeTypeFiles = mimeTypeFilesField.get(MIME_TYPES_MAP);
      Set<String> mimeTypes = Sets.newHashSet();
      for (int i = 0; i < Array.getLength(mimeTypeFiles); i++) {
        Object mimeTypeFile = Array.get(mimeTypeFiles, i);
        if (mimeTypeFile != null) {
          Map<?, ?> typeHash = (Map) typeHashField.get(mimeTypeFile);
          Iterables.addAll(mimeTypes, Iterables.transform(typeHash.values(), getMimeTypeFunction));
        }
      }
      return ImmutableSet.copyOf(mimeTypes);
    } catch (ReflectiveOperationException | NullPointerException ex) {
      return ImmutableSet.of();
    }
  }

  private static Field makeFieldAccessible(Class<?> clazz, String fieldName) throws ReflectiveOperationException {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    return field;
  }

  private static class GetMimeTypeFunction implements Function<Object, String> {
    private final Method getMIMEType;

    GetMimeTypeFunction() throws ReflectiveOperationException {
      this.getMIMEType = Class.forName("com.sun.activation.registries.MimeTypeEntry").getMethod("getMIMEType");
    }

    @Override
    public String apply(Object entry) {
      try {
        return (String) getMIMEType.invoke(entry);
      } catch (ReflectiveOperationException ex) {
        throw new NullPointerException("Could not get mime type: " + ex.getMessage());
      }
    }
  }

  public static ImmutableSet<String> getDefaultExcludedMimeTypes() {
    Set<String> knownMimeTypes = extractKnownMimeTypes();

    Set<String> compressedArchiveTypes = ImmutableSet.of("application/compress", "application/zip", "application/gzip");
    Iterable<String> knownExceptNonXmlImagesAudioAndVideo = Iterables.filter(knownMimeTypes,
      type -> (type.startsWith("image/") || type.startsWith("audio/") || type.startsWith("video/")) && !type.endsWith("+xml")
    );

    return ImmutableSet.<String>builder()
      .addAll(compressedArchiveTypes)
      .addAll(knownExceptNonXmlImagesAudioAndVideo)
      .build();
  }

}
