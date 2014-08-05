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

package ratpack.form.internal;

import com.google.common.collect.ListMultimap;
import ratpack.api.Nullable;
import ratpack.form.Form;
import ratpack.form.UploadedFile;
import ratpack.util.MultiValueMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("NullableProblems")
public class DefaultForm implements Form {

  private final MultiValueMap<String, String> attributes;
  private final MultiValueMap<String, UploadedFile> files;

  public DefaultForm(MultiValueMap<String, String> attributes, MultiValueMap<String, UploadedFile> files) {
    this.attributes = attributes;
    this.files = files;
  }

  @Override
  public UploadedFile file(String name) {
    return files.get(name);
  }

  @Override
  public List<UploadedFile> files(String name) {
    return files.getAll(name);
  }

  @Override
  public MultiValueMap<String, UploadedFile> files() {
    return files;
  }

  @Override
  public List<String> getAll(String key) {
    return attributes.getAll(key);
  }

  @Override
  public Map<String, List<String>> getAll() {
    return attributes.getAll();
  }

  @Override
  @Nullable
  public String get(Object key) {
    return attributes.get(key);
  }

  @Override
  public String put(String key, String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String remove(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  @SuppressWarnings("NullableProblems")
  public void putAll(Map<? extends String, ? extends String> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    return attributes.size();
  }

  @Override
  public boolean isEmpty() {
    return attributes.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return attributes.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return attributes.containsValue(value);
  }

  @Override
  public Set<String> keySet() {
    return attributes.keySet();
  }

  @Override
  public Collection<String> values() {
    return attributes.values();
  }

  @Override
  public Set<Entry<String, String>> entrySet() {
    return attributes.entrySet();
  }

  @Override
  public String toString() {
    return attributes.toString();
  }

  @Override
  public ListMultimap<String, String> asMultimap() {
    return attributes.asMultimap();
  }
}
