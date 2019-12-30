package sipka.cmdline.processor.model;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class ModelCommonConverter {
	private TypeMirror targetType;
	private TypeElement methodDeclaringType;
	private String methodName;

	public ModelCommonConverter(TypeMirror targetType, TypeElement methodDeclaringType, String methodName) {
		this.targetType = targetType;
		this.methodDeclaringType = methodDeclaringType;
		this.methodName = methodName;
	}

	public TypeMirror getTargetType() {
		return targetType;
	}

	public TypeElement getMethodDeclaringType() {
		return methodDeclaringType;
	}

	public String getMethodName() {
		return methodName;
	}
}
