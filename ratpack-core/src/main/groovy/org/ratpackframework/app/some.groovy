import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.customizers.CompilationCustomizer

class Thing {
  String prop = "foo"
}

abstract class CustomScript extends Script {

  void doStuff(@DelegatesTo(Thing) Closure c) {
    c.resolveStrategy = Closure.DELEGATE_FIRST
    c.delegate = new Thing()
    c()
  }

}

final CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
compilerConfiguration.setScriptBaseClass(CustomScript.getName());
compilerConfiguration.addCompilationCustomizers(new CompilationCustomizer(CompilePhase.CONVERSION) {
  @Override
  public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws org.codehaus.groovy.control.CompilationFailedException {
    classNode.addAnnotation(new AnnotationNode(new ClassNode(CompileStatic.class)));
  }
});

def classLoader = new GroovyClassLoader(getClass().classLoader, compilerConfiguration)
def clazz = classLoader.parseClass("""

  doStuff {
    println prop
  }

""")


clazz.newInstance().run()

