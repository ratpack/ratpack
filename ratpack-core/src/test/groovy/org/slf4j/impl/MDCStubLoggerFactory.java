package org.slf4j.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.ILoggerFactory;

public class MDCStubLoggerFactory implements ILoggerFactory {

  ConcurrentMap<String, Logger> loggerMap;

  public MDCStubLoggerFactory() {
    loggerMap = new ConcurrentHashMap<String, Logger>();
  }

  public Logger getLogger(String name) {
    Logger simpleLogger = loggerMap.get(name);
    if (simpleLogger != null) {
      return simpleLogger;
    } else {
      Logger newInstance = new MDCStubLogger(name);
      Logger oldInstance = loggerMap.putIfAbsent(name, newInstance);
      return oldInstance == null ? newInstance : oldInstance;
    }
  }

  void reset() {
    loggerMap.clear();
  }
}
