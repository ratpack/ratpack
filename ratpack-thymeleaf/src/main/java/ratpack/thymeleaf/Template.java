package ratpack.thymeleaf;

import org.thymeleaf.context.Context;

import java.util.Map;

public class Template {

  private final String name;
  private final Context model;

  private final String contentType;

  public String getName() {
    return name;
  }

  public Context getModel() {
    return model;
  }

  public String getContentType() {
    return contentType;
  }

  private Template(String name, Context model, String contentType) {
    this.name = name;
    this.model = model;
    this.contentType = contentType;
  }

  public static Template thymeleafTemplate(String name) {
    return thymeleafTemplate(null, name);
  }

  public static Template thymeleafTemplate(Map<String, ?> model, String name) {
    return thymeleafTemplate(model, name, null);
  }

  public static Template thymeleafTemplate(Map<String, ?> model, String name, String contentType) {
    Context context = new Context();
    if (model != null) {
      context.setVariables(model);
    }

    return new Template(name, context, contentType);
  }
}
