package org.ratpackframework.bootstrap.internal;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.ratpackframework.error.DefaultErrorHandler;
import org.ratpackframework.error.DefaultNotFoundHandler;
import org.ratpackframework.error.ErroredHttpExchange;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

import java.util.List;

public class RootModule extends AbstractModule {

  public static final String MAIN_APP_HTTP_HANDLER = "mainAppHttpHandler";
  public static final String MAIN_STATIC_ASSET_HTTP_HANDLER = "mainStaticAssetHttpHandler";

  public static final String MAIN_HTTP_ERROR_HANDLER = "mainHttpErrorHandler";
  public static final String MAIN_NOT_FOUND_HTTP_HANDLER = "mainNotFoundHttpHandler";

  public static final TypeLiteral<Handler<Routed<HttpExchange>>> HTTP_HANDLER = new TypeLiteral<Handler<Routed<HttpExchange>>>() {
  };
  public static final TypeLiteral<Handler<ErroredHttpExchange>> HTTP_ERROR_HANDLER = new TypeLiteral<Handler<ErroredHttpExchange>>() {
  };
  public static final TypeLiteral<List<Handler<Routed<HttpExchange>>>> HTTP_HANDLER_PIPELINE = new TypeLiteral<List<Handler<Routed<HttpExchange>>>>() {
  };
  public static final TypeLiteral<List<String>> STRING_LIST = new TypeLiteral<List<String>>() {
  };


  @Override
  protected void configure() {
    bind(HTTP_ERROR_HANDLER).annotatedWith(Names.named(MAIN_HTTP_ERROR_HANDLER)).to(DefaultErrorHandler.class);
    bind(HTTP_HANDLER).annotatedWith(Names.named(MAIN_NOT_FOUND_HTTP_HANDLER)).to(DefaultNotFoundHandler.class);
  }

}
