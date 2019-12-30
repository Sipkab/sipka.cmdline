package sipka.cmdline.processor.model;

import javax.lang.model.type.TypeMirror;

public class ModelMultiParameter {

	private TypeMirror elementType;
	private String methodName;

	public ModelMultiParameter(TypeMirror elementType, String methodName) {
		this.elementType = elementType;
		this.methodName = methodName;
	}

	public TypeMirror getElementType() {
		return elementType;
	}

	public String getMethodName() {
		return methodName;
	}
}
