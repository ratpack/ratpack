package ratpack.thymeleaf.internal;

import org.thymeleaf.TemplateEngine;
import ratpack.file.MimeTypes;
import ratpack.handling.Context;
import ratpack.render.RendererSupport;
import ratpack.thymeleaf.Template;

import javax.inject.Inject;

public class ThymeleafTemplateRenderer extends RendererSupport<Template> {

    private final TemplateEngine thymeleaf;

    @Inject
    public ThymeleafTemplateRenderer(TemplateEngine thymeleaf) {
        this.thymeleaf = thymeleaf;
    }

    @Override
    public void render(Context context, Template template) {
        String contentType = template.getContentType();
        contentType = contentType == null ? context.get(MimeTypes.class).getContentType(template.getName()) : contentType;
        try {
            context.getResponse().send(contentType, thymeleaf.process(template.getName(), template.getModel()));
        }
        catch (Exception e) {
            context.error(e);
        }
    }
}
