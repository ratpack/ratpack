package org.ratpackframework.guice;

import com.google.inject.AbstractModule;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;


public class RequestScopeModule extends AbstractModule {

  @Override
  protected void configure() {
    RequestScope requestScope = new RequestScope();
    bindScope(RequestScoped.class, requestScope);
    bind(RequestScope.class).toInstance(requestScope);

    bind(Request.class).toProvider(RequestScope.<Request>seededKeyProvider()).in(RequestScoped.class);
    bind(Response.class).toProvider(RequestScope.<Response>seededKeyProvider()).in(RequestScoped.class);
  }

}
