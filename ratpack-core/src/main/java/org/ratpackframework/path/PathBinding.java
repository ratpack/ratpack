package org.ratpackframework.path;

import org.ratpackframework.Nullable;

public interface PathBinding {

  PathContext bind(String path, @Nullable PathContext pathContext);

}
