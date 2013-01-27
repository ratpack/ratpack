package org.ratpackframework.templating;

import java.util.Map;

public interface TemplateModel extends Map<String, Object> {

  <K, V> Map<K, V> map(String key, Class<K> keyType, Class<V> valueType);

  Map<String, Object> map(String key);

}
