package japicmp.output.semver;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import japicmp.config.Options;
import japicmp.model.AccessModifier;
import japicmp.model.JApiAnnotation;
import japicmp.model.JApiAnnotationElement;
import japicmp.model.JApiBinaryCompatibility;
import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiClass;
import japicmp.model.JApiConstructor;
import japicmp.model.JApiField;
import japicmp.model.JApiHasAnnotations;
import japicmp.model.JApiHasChangeStatus;
import japicmp.model.JApiImplementedInterface;
import japicmp.model.JApiMethod;
import japicmp.model.JApiSuperclass;
import japicmp.output.Filter;
import japicmp.output.OutputFilter;
import japicmp.output.OutputGenerator;

public class SemverOut extends OutputGenerator {

	private enum SemverStatus {
		UNCHANGED, CHANGED_BINARY_COMPATIBLE, CHANGED_BINARY_INCOMPATIBLE;
	}

	public SemverOut(Options options, List<JApiClass> jApiClasses) {
		super(options, jApiClasses);
	}

	@Override
	public void generate() {
		System.out.println(value());
	}

	public String value() {
		final ImmutableSet.Builder<SemverStatus> builder = ImmutableSet.builder();
		Filter.filter(jApiClasses, new Filter.FilterVisitor() {
			@Override
			public void visit(Iterator<JApiClass> iterator, JApiClass jApiClass) {
				builder.add(signs(jApiClass));
			}

			@Override
			public void visit(Iterator<JApiMethod> iterator, JApiMethod jApiMethod) {
				builder.add(signs(jApiMethod));
			}

			@Override
			public void visit(Iterator<JApiConstructor> iterator, JApiConstructor jApiConstructor) {
				builder.add(signs(jApiConstructor));
			}

			@Override
			public void visit(Iterator<JApiImplementedInterface> iterator, JApiImplementedInterface jApiImplementedInterface) {
				builder.add(signs(jApiImplementedInterface));
			}

			@Override
			public void visit(Iterator<JApiField> iterator, JApiField jApiField) {
				builder.add(signs(jApiField));
			}

			@Override
			public void visit(Iterator<JApiAnnotation> iterator, JApiAnnotation jApiAnnotation) {
				builder.add(signs(jApiAnnotation));
			}

			@Override
			public void visit(JApiSuperclass jApiSuperclass) {
				builder.add(signs(jApiSuperclass));
			}
		});
		ImmutableSet<SemverStatus> build = builder.build();
		if (build.contains(SemverStatus.CHANGED_BINARY_INCOMPATIBLE)) {
			return "1.0.0";
		} else if (build.contains(SemverStatus.CHANGED_BINARY_COMPATIBLE)) {
			return "0.1.0";
		} else if (build.isEmpty() || build.contains(SemverStatus.UNCHANGED)) {
			return "0.0.1";
		} else {
			return "N/A";
		}
	}

	private SemverStatus signs(JApiHasChangeStatus hasChangeStatus) {
		JApiChangeStatus changeStatus = hasChangeStatus.getChangeStatus();
		switch (changeStatus) {
			case UNCHANGED:
				return SemverStatus.UNCHANGED;
			case NEW:
			case REMOVED:
			case MODIFIED:
				if (hasChangeStatus instanceof JApiBinaryCompatibility) {
					JApiBinaryCompatibility binaryCompatibility = (JApiBinaryCompatibility) hasChangeStatus;
					if (binaryCompatibility.isBinaryCompatible()) {
						return SemverStatus.CHANGED_BINARY_COMPATIBLE;
					} else {
						return SemverStatus.CHANGED_BINARY_INCOMPATIBLE;
					}
				} else {
					throw new IllegalStateException("Element '" + hasChangeStatus.getClass().getCanonicalName() + " does not implement '" + JApiBinaryCompatibility.class.getCanonicalName() + "'.");
				}
			default:
				throw new IllegalStateException("The following JApiChangeStatus is not supported: " + (changeStatus == null ? "null" : changeStatus.name()));
		}
	}
}
