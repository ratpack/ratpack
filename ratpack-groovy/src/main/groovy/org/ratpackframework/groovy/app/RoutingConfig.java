package org.ratpackframework.groovy.app;

import java.io.File;

public class RoutingConfig {

  private File script;
  private boolean staticallyCompile;
  private boolean reloadable = true;

  public RoutingConfig() {
    this(new File(System.getProperty("user.dir"), "ratpack.groovy"));
  }

  public RoutingConfig(File script) {
    this.script = script;
  }

  public File getScript() {
    return script;
  }

  public void setScript(File script) {
    this.script = script;
  }

  public boolean isStaticallyCompile() {
    return staticallyCompile;
  }

  public void setStaticallyCompile(boolean staticallyCompile) {
    this.staticallyCompile = staticallyCompile;
  }

  public boolean isReloadable() {
    return reloadable;
  }

  public void setReloadable(boolean reloadable) {
    this.reloadable = reloadable;
  }
}
