package ratpack.thymeleaf;

import org.thymeleaf.context.Context;

import java.util.Map;

public class Template<T> {

  private final String name;
  private final T model;

  private final String contentType;

  public String getName() {
    return name;
  }

  public T getModel() {
    return model;
  }

  public String getContentType() {
    return contentType;
  }

  private Template(String name, T model, String contentType) {
    this.name = name;
    this.model = model;
    this.contentType = contentType;
  }

  public static Template<Context> thymeleafTemplate(String name) {
    return thymeleafTemplate(null, name);
  }

  public static Template<Context> thymeleafTemplate(Map<String, ?> model, String name) {
    return thymeleafTemplate(model, name, null);
  }

  public static Template<Context> thymeleafTemplate(Map<String, ?> model, String name, String contentType) {
    Context context = new Context();
    if (model != null) {
      context.setVariables(model);
    }

    return new Template<>(name, context, contentType);
  }
}
