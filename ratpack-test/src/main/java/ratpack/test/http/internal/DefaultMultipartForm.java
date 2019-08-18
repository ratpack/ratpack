/*
 * Copyright 2018 the original author or authors.
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

package ratpack.test.http.internal;

import ratpack.test.http.MultipartFileSpec;
import ratpack.test.http.MultipartFormSpec;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
    As defined in RFC 1867 and 2388 (see https://tools.ietf.org/html/rfc1867)
 */
public class DefaultMultipartForm {

  private final Data data;
  private final Writer writer;
  private final Map<String, String> _headers;

  public static Builder builder() {
    return new Builder();
  }

  private DefaultMultipartForm(Data data) {
    this.data = data;
    this.writer = new Writer(data);
    this._headers = new HashMap<>();
    _headers.put("Content-Type", getContentType());
  }

  public String getBody() {
    return writer.write();
  }

  public String getContentType() {
    return String.format("multipart/form-data; boundary=%s", data.boundary);
  }

  public Map<String, String> getHeaders() {
    return Collections.unmodifiableMap(_headers);
  }

  final static class Writer {

    private final Data data;

    Writer(Data data) {
      this.data = data;
    }

    String write() {
      StringBuilder result = new StringBuilder();

      addFields(data.fields, data.boundary, result);
      addFiles(data.files, data.boundary, result);
      closeBoundary(data.boundary, result);

      return result.toString();
    }

    private void addFields(Map<String, Set<String>> fields, String boundary, StringBuilder out) {
      fields.forEach((name, values) -> {
        values.forEach((value) -> {
          openBoundary(boundary, out);
          declareField(name, out);
          data(value.getBytes(), out);
        });
      });
    }

    private void addFiles(Map<String, Set<File>> files, String boundary, StringBuilder out) {
      files.forEach((name, filesByField) -> {
        openBoundary(boundary, out);
        if (filesByField.size() > 1) {
          addMultipleFilesToField(filesByField, name, out);
        } else {
          addSingleFileToField(filesByField, out);
        }
      });
    }

    private void addMultipleFilesToField(Set<File> files, String name, StringBuilder out) {
      String subBoundary = generateSubBoundary(data.boundary, name);

      declareField(name, out);
      type(String.format("multipart/mixed, boundary=%s\n", subBoundary), out);
      files.forEach((file) -> {
        openBoundary(subBoundary, out);
        disposition(String.format("attachment; filename=\"%s\"", file.getName()), out);
        file(file, out);
      });
      closeBoundary(subBoundary, out);
    }

    private void addSingleFileToField(Set<File> files, StringBuilder out) {
      File file = files.stream().findFirst().get();
      disposition(String.format("form-data; name=\"%s\"; filename=\"%s\"",
        file.getField(), file.getName()), out);
      file(file, out);
    }

    private void data(byte[] value, StringBuilder out) {
      out.append("\n");
      out.append(new String(value));
      out.append("\n");
    }

    private void declareField(String name, StringBuilder out) {
      disposition(String.format("form-data; name=\"%s\"", name), out);
    }

    private void disposition(String value, StringBuilder out) {
      label("Content-Disposition", value, out);
    }

    private void encoding(String value, StringBuilder out) {
      if (value != null && !value.isEmpty()) {
        label("Content-Transfer-Encoding", value, out);
      }
    }

    private void file(File file, StringBuilder out) {
      type(file.getContentType(), out);
      encoding(file.getEncoding(), out);
      data(file.getData(), out);
    }

    private String generateSubBoundary(String boundary, String name) {
      return String.format("%s_%s", boundary, name);
    }

    private void type(String value, StringBuilder out) {
      label("Content-Type", value, out);
    }

    private void label(String label, String value, StringBuilder out) {
      out.append(label);
      out.append(": ");
      out.append(value);
      out.append("\n");
    }

    private void openBoundary(String boundary, StringBuilder out) {
      out.append("--");
      out.append(boundary);
      out.append("\n");
    }

    private void closeBoundary(String boundary, StringBuilder out) {
      out.append("--");
      out.append(boundary);
      out.append("--\n");
    }

  }

  final static class Data {

    final String boundary;
    final Map<String, Set<String>> fields;
    final Map<String, Set<File>> files;

    Data(String boundary, Map<String, Set<String>> fields, Map<String, Set<File>> files) {
      this.boundary = boundary;
      this.fields = Collections.unmodifiableMap(fields);
      this.files = Collections.unmodifiableMap(files);
    }

  }

  public final static class Builder implements MultipartFormSpec {

    private final String _boundary;
    private final Map<String, Set<String>> _fields;
    private final Map<String, Set<File>> _files;

    private Builder() {
      this._boundary = generateBoundary();
      this._fields = new TreeMap<>();
      this._files = new TreeMap<>();
    }

    public String getBoundary() {
      return _boundary;
    }

    public Map<String, Set<String>> getFields() {
      return Collections.unmodifiableMap(_fields);
    }

    public Map<String, Set<File>> getFiles() {
      return Collections.unmodifiableMap(_files);
    }

    public DefaultMultipartForm build() {
      return new DefaultMultipartForm(new Data(_boundary, _fields, _files));
    }

    public Builder field(String name, String value) {
      if (!_fields.containsKey(name)) {
        _fields.put(name, new TreeSet<>());
      }
      _fields.get(name).add(value);

      return this;
    }

    public Builder fields(Map<String, String> data) {
      data.forEach(this::field);

      return this;
    }

    public FileBuilder file() {
      return new FileBuilder(this);
    }

    private Builder add(File file) {
      String name = file.getField();
      if (!_files.containsKey(name)) {
        _files.put(name, new TreeSet<>());
      }
      _files.get(name).add(file);

      return this;
    }

    private static String generateBoundary() {
      return String.format("TEST%d", System.currentTimeMillis());
    }

  }

  public final static class FileBuilder implements MultipartFileSpec {

    private final Builder _builder;

    private String _contentType;
    private byte[] _data;
    private String _encoding;
    private String _field;
    private String _name;

    private FileBuilder(Builder builder) {
      this._builder = builder;
      this._contentType = "text/plain";
      this._field = "file";
      this._name = "filename.txt";
    }

    public Builder add() {
      File file = new File(_field, _contentType, _encoding, _name, _data);
      file.validate();

      return _builder.add(file);
    }

    public FileBuilder contentType(String contentType) {
      this._contentType = contentType;

      return this;
    }

    public FileBuilder data(byte[] data) {
      this._data = data;

      return this;
    }

    public FileBuilder data(String data) {
      try {
        this._data = data.getBytes("UTF-8");
      } catch(UnsupportedEncodingException uee) {
        throw new IllegalArgumentException(uee);
      }

      return this;
    }

    public FileBuilder encoding(String encoding) {
      this._encoding = encoding;

      return this;
    }

    public FileBuilder field(String field) {
      this._field = field;

      return this;
    }

    public FileBuilder name(String name) {
      this._name = name;

      return this;
    }

  }

  public final static class File implements Comparable<File> {

    private final String _contentType;
    private final byte[] _data;
    private final String _encoding;
    private final String _field;
    private final String _name;

    private File(String field, String contentType, String encoding, String name, byte[] data) {
      this._contentType = contentType;
      this._data = data;
      this._encoding = encoding;
      this._field = field;
      this._name = name;
    }

    public String getContentType() {
      return _contentType;
    }

    public byte[] getData() {
      return _data;
    }

    public String getEncoding() {
      return _encoding;
    }

    public String getField() {
      return _field;
    }

    public String getName() {
      return _name;
    }

    @Override
    public int compareTo(File file) {
      int result = this._field.compareTo(file._field);
      if (result == 0) {
        result = this._name.compareTo(file._name);
      }

      return result;
    }

    private void validate() {
      if (!(isValueSet(_contentType) && isValueSet(_data) && isValueSet(_field) && isValueSet(_name))) {
        throw new IllegalArgumentException(
          String.format("File requires contentType, name and data (%s, %s, %s, %s)",
            _field, _contentType, _name, _data));
      }
      if (isValueSet(_encoding) && !Encoding.isValid(_encoding)) {
        throw new IllegalArgumentException(
          String.format("Invalid content transfer encoding: [%s]", _encoding));
      }
    }

    private boolean isValueSet(String value) {
      return value != null && !value.isEmpty();
    }

    private boolean isValueSet(byte[] value) {
      return value != null && !(value.length == 0);
    }

  }

  public final static class Encoding {

    final static List<String> VALUES = Stream.of("BASE64", "QUOTED-PRINTABLE", "8BIT", "7BIT", "BINARY")
      .collect(Collectors.toList());

    static boolean isValid(String value) {
      return VALUES.stream().anyMatch((constant)
        -> value.equalsIgnoreCase(constant) || value.toUpperCase().startsWith("X-"));
    }

  }

}
