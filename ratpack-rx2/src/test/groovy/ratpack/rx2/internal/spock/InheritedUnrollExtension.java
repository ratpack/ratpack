package ratpack.rx2.internal.spock;

import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension;
import org.spockframework.runtime.extension.builtin.UnrollNameProvider;
import org.spockframework.runtime.model.FeatureInfo;
import org.spockframework.runtime.model.IterationInfo;
import org.spockframework.runtime.model.NameProvider;
import org.spockframework.runtime.model.SpecInfo;

public final class InheritedUnrollExtension extends AbstractAnnotationDrivenExtension<InheritedUnroll> {

  @Override
  public void visitSpecAnnotation(InheritedUnroll unroll, SpecInfo spec) {
    spec.getFeatures()
      .stream()
      .filter(FeatureInfo::isParameterized)
      .forEach(feature -> visitFeatureAnnotation(unroll, feature));
  }

  @Override
  public void visitFeatureAnnotation(InheritedUnroll unroll, FeatureInfo feature) {
    if (feature.isParameterized()) {
      feature.setReportIterations(true);
      feature.setIterationNameProvider(chooseNameProvider(unroll, feature));
    }
  }

  private NameProvider<IterationInfo> chooseNameProvider(InheritedUnroll unroll, FeatureInfo feature) {
    if (unroll.value().length() > 0) {
      return new UnrollNameProvider(feature, unroll.value());
    } else if (feature.getName().contains("#")) {
      return new UnrollNameProvider(feature, feature.getName());
    } else {
      return null;
    }
  }

}

