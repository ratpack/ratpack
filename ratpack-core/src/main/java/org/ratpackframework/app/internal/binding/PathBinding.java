package org.ratpackframework.app.internal.binding;

import java.util.Map;

public interface PathBinding {

  Map<String, String> map(String path);

}
