/*
 * Copyright 2014 the original author or authors.
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

package ratpack.func;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ratpack.func.Function.conditional;
import static ratpack.func.Function.constant;

public class FunctionTest {

  @Test
  public void testAndThen() throws Exception {
    Function<String, String> f = String::toUpperCase;
    f = f.andThen(in -> in + "-BAR");
    assertEquals("FOO-BAR", f.apply("foo"));
  }

  @Test
  public void testCompose() throws Exception {
    Function<String, String> f = String::toUpperCase;
    f = f.compose(in -> in + "-bar");
    assertEquals("FOO-BAR", f.apply("foo"));
  }

  @Test
  public void testConditional() throws Exception {
    Function<Integer, String> f1 = Function.when(i -> i == 1, i -> "1", i -> "2");
    assertEquals("1", f1.apply(1));
    assertEquals("2", f1.apply(10));

    Function<String, Integer> f2 = conditional(constant(3), s -> s
      .when(i -> i.length() < 2, i -> 2)
      .when(i -> i.length() < 1, i -> 1)
    );

    assertEquals(Integer.valueOf(2), f2.apply("a"));
    assertEquals(Integer.valueOf(3), f2.apply("aaaa"));
  }

}
