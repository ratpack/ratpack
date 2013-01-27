package org.ratpackframework.templating.internal;

import java.util.Map;

public interface NestedRenderer {

  NestedTemplate render(String templateName, Map<String, ?> model);

}
