package ratpack.api;

import java.lang.annotation.*;


/**
 * Declares that a method or function-like method parameter is wrapped in an Operation when executed and can freely use <a href="../exec/Promise.html">Promise</a> and other async constructs.
 * <p>
 * If this annotation is present on a method, it indicates that the method may be asynchronous.
 * That is, it is not necessarily expected to have completed its logical work when the method returns.
 * The method must however use <a href="../exec/Promise.html">Promise</a>, <a href="../exec/Operation.html">Operation</a>, or other execution mechanisms to perform asynchronous work.
 * <p>
 * Most such methods are invoked as part of Ratpack.
 * If you need to invoke such a method, do so as part of a discrete <a href="../exec/Operation.html">Operation</a>.
 * <p>
 * If this annotation is present on a function type method parameter, it indicates that the annotated function may be asynchronous.
 * Similarly, if you need to invoke such a parameter, do so as part of a discrete <a href="../exec/Operation.html">Operation</a>.
 * <p>
 *
 * @since 2.0.0
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface ExecSegment {
}
