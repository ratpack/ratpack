/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.form;

import ratpack.form.internal.DefaultFormParse;

/**
 * Static utitilies for dealing with HTML forms.
 */
public class Forms {

  /**
   * Returns a parse object for {@link Form} objects.
   * <p>
   * Example usage:
   * <pre class="tested">
   * import ratpack.handling.Handler;
   * import ratpack.handling.Context;
   * import ratpack.form.Form;
   * import ratpack.form.UploadedFile;
   * import static ratpack.form.Forms.form;
   *
   * public class FormHandler implements Handler {
   *   public void handle(Context context) {
   *     context.getByMethod().post(new Runnable() {
   *       public void run() {
   *         Form form = context.parse(form());
   *         UploadedFile file = form.file("someFile.txt");
   *         String param = form.get("param");
   *         List&lt;String&gt; multi = form.getAll("multi");
   *         context.render("form uploaded!");
   *       }
   *     });
   *   }
   * }
   * </pre>
   *
   * @return A parse object, suitable for {@link ratpack.handling.Context#parse(ratpack.parse.Parse)}.
   */
  public static FormParse form() {
    return new DefaultFormParse();
  }

}
