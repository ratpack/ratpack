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

package ratpack.site.pages

import geb.Page

class HomePage extends Page {

  static url = "/"

  static at = { title == "Ratpack: Lean & powerful HTTP apps for the JVM" }

  static content = {
    promoNavLink { text -> $('#promo nav a', text: text) }
    manualLink(to: ManualPage, wait: true) { promoNavLink("Current Manual") }
    versionsLink(to: VersionsPage, wait: true)  { promoNavLink("All Versions") }
  }
}
