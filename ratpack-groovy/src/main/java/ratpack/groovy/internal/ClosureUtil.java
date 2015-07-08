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

package ratpack.groovy.internal;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.func.Action;
import ratpack.groovy.script.internal.LineNumber;
import ratpack.groovy.script.internal.ScriptPath;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;

public abstract class ClosureUtil {

  private ClosureUtil() {
  }

  @SuppressWarnings("UnusedDeclaration") // used in GroovyChainDslFixture
  public static <D, R> R configureDelegateOnly(@DelegatesTo.Target D delegate, @DelegatesTo(strategy = Closure.DELEGATE_ONLY) Closure<R> configurer) {
    return configure(delegate, delegate, configurer, Closure.DELEGATE_ONLY);
  }

  public static <D, R> R configureDelegateFirst(@DelegatesTo.Target D delegate, @DelegatesTo(strategy = Closure.DELEGATE_FIRST) Closure<R> configurer) {
    return configure(delegate, delegate, configurer, Closure.DELEGATE_FIRST);
  }

  public static <D, A, R> R configureDelegateFirst(@DelegatesTo.Target D delegate, A argument, @DelegatesTo(strategy = Closure.DELEGATE_FIRST) Closure<R> configurer) {
    return configure(delegate, argument, configurer, Closure.DELEGATE_FIRST);
  }

  public static <D, R> D configureDelegateFirstAndReturn(@DelegatesTo.Target D delegate, @DelegatesTo(strategy = Closure.DELEGATE_FIRST) Closure<R> configurer) {
    configure(delegate, delegate, configurer, Closure.DELEGATE_FIRST);
    return delegate;
  }

  private static <R, D, A> R configure(D delegate, A argument, Closure<R> configurer, int resolveStrategy) {
    if (configurer == null) {
      return null;
    }
    Closure<R> clone = cloneAndSetDelegate(delegate, configurer, resolveStrategy);
    if (clone.getMaximumNumberOfParameters() == 0) {
      return clone.call();
    } else {
      return clone.call(argument);
    }
  }

  public static <R, D> Closure<R> cloneAndSetDelegate(D delegate, Closure<R> configurer, int resolveStrategy) {
    @SuppressWarnings("unchecked")
    Closure<R> clone = (Closure<R>) configurer.clone();
    clone.setDelegate(delegate);
    clone.setResolveStrategy(resolveStrategy);
    return clone;
  }

  // Type token is here for in the future when @DelegatesTo supports this kind of API
  public static <T> Action<T> delegatingAction(@SuppressWarnings("UnusedParameters") Class<T> type, final Closure<?> configurer) {
    return object -> configureDelegateFirst(object, configurer);
  }

  @SuppressWarnings("unchecked")
  public static <T> Action<T> delegatingAction(final Closure<?> configurer) {
    return (Action<T>) delegatingAction(Object.class, configurer);
  }

  public static Action<Object> action(final Closure<?> closure) {
    final Closure<?> copy = closure.rehydrate(null, closure.getOwner(), closure.getThisObject());
    return new NoDelegateClosureAction(copy);
  }

  private static class NoDelegateClosureAction implements Action<Object> {

    private final Closure<?> copy;

    public NoDelegateClosureAction(Closure<?> copy) {
      this.copy = copy;
    }

    @Override
    public void execute(Object thing) throws Exception {
      copy.call(thing);
    }
  }

  private static class DelegatingClosureRunnable<D, A> implements Runnable {
    private final D delegate;
    private final A argument;
    private final Closure<?> closure;

    private DelegatingClosureRunnable(D delegate, A argument, Closure<?> closure) {
      this.delegate = delegate;
      this.argument = argument;
      this.closure = closure;
    }

    @Override
    public void run() {
      configureDelegateFirst(delegate, argument, closure);
    }
  }

  public static <D, A> Runnable delegateFirstRunnable(D delegate, A argument, Closure<?> closure) {
    return new DelegatingClosureRunnable<>(delegate, argument, closure);
  }

  public static <T> Closure<T> returning(final T thing) {
    return new PassThroughClosure<>(thing);
  }

  public static Closure<Void> noop() {
    return returning((Void) null);
  }

  private static class PassThroughClosure<T> extends Closure<T> {

    static final long serialVersionUID = 0;

    private final T thing;

    public PassThroughClosure(T thing) {
      super(null);
      this.thing = thing;
    }

    @SuppressWarnings("UnusedDeclaration")
    protected T doCall() {
      return thing;
    }

    @SuppressWarnings("UnusedDeclaration")
    protected T doCall(Object it) {
      return thing;
    }
  }

  public static Path findScript(Closure<?> closure) {
    Class<?> clazz = closure.getClass();
    ProtectionDomain protectionDomain = clazz.getProtectionDomain();
    CodeSource codeSource = protectionDomain.getCodeSource();
    URL location = codeSource.getLocation();
    URI uri;
    try {
      uri = location.toURI();
    } catch (URISyntaxException e) {
      return null;
    }

    Path path;
    if (uri.toString().startsWith("file:/groovy/")) {
      path = findScriptByAnnotation(closure);
    } else {
      path = Paths.get(uri);
    }

    if (path != null && Files.exists(path)) {
      return path;
    } else {
      return null;
    }
  }

  private static Path findScriptByAnnotation(Closure<?> closure) {
    Class<?> rootClass = getRootClass(closure);
    ScriptPath annotation = rootClass.getAnnotation(ScriptPath.class);
    if (annotation == null) {
      return null;
    } else {
      String scriptPath = annotation.value();
      URI uri;
      try {
        uri = new URI(scriptPath);
      } catch (URISyntaxException e) {
        return null;
      }
      return Paths.get(uri);
    }
  }

  private static Class<?> getRootClass(Object object) {
    Class<?> rootClass = object.getClass();
    Class<?> enclosingClass = rootClass.getEnclosingClass();
    while (enclosingClass != null) {
      rootClass = enclosingClass;
      enclosingClass = rootClass.getEnclosingClass();
    }
    return rootClass;
  }

  public static SourceInfo getSourceInfo(Closure<?> closure) {
    Class<?> closureClass = closure.getClass();
    LineNumber lineNumber = closureClass.getAnnotation(LineNumber.class);
    if (lineNumber == null) {
      return null;
    }

    Class<?> rootClass = getRootClass(closure);
    ScriptPath scriptPath = rootClass.getAnnotation(ScriptPath.class);
    if (scriptPath == null) {
      return null;
    }

    return new SourceInfo(scriptPath.value(), lineNumber.value());
  }

  public static class SourceInfo {
    private final String uri;
    private final int lineNumber;

    public SourceInfo(String uri, int lineNumber) {
      this.uri = uri;
      this.lineNumber = lineNumber;
    }

    public String getUri() {
      return uri;
    }

    public int getLineNumber() {
      return lineNumber;
    }
  }
}
