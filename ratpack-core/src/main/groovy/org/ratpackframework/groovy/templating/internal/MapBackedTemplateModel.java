package org.ratpackframework.groovy.templating.internal;

import org.ratpackframework.groovy.templating.TemplateModel;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MapBackedTemplateModel implements TemplateModel {

  private final Map<String, Object> backing;

  public MapBackedTemplateModel(Map<String, Object> backing) {
    this.backing = backing;
  }

  @Override
  public int size() {
    return backing.size();
  }

  @Override
  public boolean isEmpty() {
    return backing.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return backing.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return backing.containsValue(value);
  }

  @Override
  public Object get(Object key) {
    return backing.get(key);
  }

  @Override
  public Object put(String key, Object value) {
    return backing.put(key, value);
  }

  @Override
  public Object remove(Object key) {
    return backing.remove(key);
  }

  @SuppressWarnings("NullableProblems")
  @Override
  public void putAll(Map<? extends String, ?> m) {
    backing.putAll(m);
  }

  @Override
  public void clear() {
    backing.clear();
  }

  @SuppressWarnings("NullableProblems")
  @Override
  public Set<String> keySet() {
    return backing.keySet();
  }

  @SuppressWarnings("NullableProblems")
  @Override
  public Collection<Object> values() {
    return backing.values();
  }

  @SuppressWarnings("NullableProblems")
  @Override
  public Set<Entry<String, Object>> entrySet() {
    return backing.entrySet();
  }

  @Override
  public <K, V> Map<K, V> map(String key, Class<K> keyType, Class<V> valueType) {
    Object value = get(key);
    if (value == null) {
      return null;
    }

    @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
    Map<K, V> mapValue = (Map<K, V>) value;
    return mapValue;
  }

  @Override
  public Map<String, Object> map(String key) {
    return map(key, String.class, Object.class);
  }
}
