# Java 9 Support

This is a list of known issues, caveats, and workarounds to use early Java 9+ support. While Java 9 support is far from
complete, core functionality appears to work with only a few warnings and messages here and there.

## Known Library Caveats

The following are known version conflicts or requirements for Ratpack to run seamlessly on Java 9.

* `ratpack-groovy` requires Groovy 2.5.2 or higher per issue [#1411](https://www.github.com/ratpack/ratpack/issues/1411)
* `ratpack-groovy-test` requires Groovy 2.5.2 or higher per issue [#1411](https://www.github.com/ratpack/ratpack/issues/1411)

## Known Issues

The following are issues caused by Java 9 and the underlying components to Ratpack at this time. Workarounds will be provided in each case


 
## Known Java 9 Errors/Warning messages

The following are messages emitted by Java 9+ that are expected.

* Guice illegal reflective access in issue [google/guice#1133](https://www.github.com/google/guice/issues/1133):
  
>  WARNING: An illegal reflective access operation has occurred  
  WARNING: Illegal reflective access by com.google.inject.internal.cglib.core.$ReflectUtils$1 (file:[User Gradle Cache Directory]/modules-2/files-2.1/com.google.inject/guice/[version hash]/[guice version jar]]  
  WARNING: Please consider reporting this to the maintainers of com.google.inject.internal.cglib.core.$ReflectUtils$1  
  WARNING: Use --illegal-access=warn to enable warnings on further illegal reflective access operations  
  WARNING: All illegal access operations will be denied in a future release  

* Netty debug messages described in issue [#1410](https://www.github.com/ratpack/ratpack/issues/1410): 

>  11:04:44.477 [main] DEBUG i.n.u.i.PlatformDependent0 - -Dio.netty.noUnsafe: false  
  11:04:44.477 [main] DEBUG i.n.u.i.PlatformDependent0 - Java version: 11  
  11:04:44.479 [main] DEBUG i.n.u.i.PlatformDependent0 - sun.misc.Unsafe.theUnsafe: available  
  11:04:44.479 [main] DEBUG i.n.u.i.PlatformDependent0 - sun.misc.Unsafe.copyMemory: available  
  11:04:44.480 [main] DEBUG i.n.u.i.PlatformDependent0 - java.nio.Buffer.address: available  
  11:04:44.483 [main] DEBUG i.n.u.i.PlatformDependent0 - direct buffer constructor: unavailable  
  java.lang.UnsupportedOperationException: Reflective setAccessible(true) disabled  
  at io.netty.util.internal.ReflectionUtil.trySetAccessible(ReflectionUtil.java:31)  
  at io.netty.util.internal.PlatformDependent0$4.run(PlatformDependent0.java:224)  
  at java.base/java.security.AccessController.doPrivileged(Native Method)  
  at io.netty.util.internal.PlatformDependent0.(PlatformDependent0.java:218)  
  at io.netty.util.internal.PlatformDependent.isAndroid(PlatformDependent.java:212)  
  at io.netty.util.internal.PlatformDependent.(PlatformDependent.java:80)  
  at io.netty.util.ConstantPool.(ConstantPool.java:32)  
  at io.netty.util.AttributeKey$1.(AttributeKey.java:27)  
  at io.netty.util.AttributeKey.(AttributeKey.java:27)  
  at ratpack.server.internal.DefaultRatpackServer.(DefaultRatpackServer.java:69)  
  at ratpack.server.RatpackServer.of(RatpackServer.java:81)  
  at ratpack.server.RatpackServer.start(RatpackServer.java:92)  
  at ratpack.groovy.GroovyRatpackMain.main(GroovyRatpackMain.java:38)  
  11:04:44.484 [main] DEBUG i.n.u.i.PlatformDependent0 - java.nio.Bits.unaligned: available, true  
  11:04:44.485 [main] DEBUG i.n.u.i.PlatformDependent0 - jdk.internal.misc.Unsafe.allocateUninitializedArray(int): unavailable  
  java.lang.IllegalAccessException: class io.netty.util.internal.PlatformDependent0$6 cannot access class jdk.internal.misc.Unsafe (in module java.base) because module java.base does not export jdk.internal.misc to unnamed module @366647c2  
  at java.base/jdk.internal.reflect.Reflection.newIllegalAccessException(Reflection.java:361)  
  at java.base/java.lang.reflect.AccessibleObject.checkAccess(AccessibleObject.java:591)  
  at java.base/java.lang.reflect.Method.invoke(Method.java:558)  
  at io.netty.util.internal.PlatformDependent0$6.run(PlatformDependent0.java:334)  
  at java.base/java.security.AccessController.doPrivileged(Native Method)  
  at io.netty.util.internal.PlatformDependent0.(PlatformDependent0.java:325)  
  at io.netty.util.internal.PlatformDependent.isAndroid(PlatformDependent.java:212)  
  at io.netty.util.internal.PlatformDependent.(PlatformDependent.java:80)  
  at io.netty.util.ConstantPool.(ConstantPool.java:32)  
  at io.netty.util.AttributeKey$1.(AttributeKey.java:27)  
  at io.netty.util.AttributeKey.(AttributeKey.java:27)  
  at ratpack.server.internal.DefaultRatpackServer.(DefaultRatpackServer.java:69)  
  at ratpack.server.RatpackServer.of(RatpackServer.java:81)  
  at ratpack.server.RatpackServer.start(RatpackServer.java:92)  
  at ratpack.groovy.GroovyRatpackMain.main(GroovyRatpackMain.java:38)  
  11:04:44.485 [main] DEBUG i.n.u.i.PlatformDependent0 - java.nio.DirectByteBuffer.(long, int): unavailable  
  11:04:44.485 [main] DEBUG i.n.u.internal.PlatformDependent - sun.misc.Unsafe: available  
 
