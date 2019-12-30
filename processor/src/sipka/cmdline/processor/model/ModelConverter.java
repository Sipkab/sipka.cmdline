package sipka.cmdline.processor.model;

import javax.lang.model.element.TypeElement;

public class ModelConverter {
	private TypeElement methodDeclaringType;
	private String methodName;

	public ModelConverter(TypeElement methodDeclaringType, String methodName) {
		this.methodDeclaringType = methodDeclaringType;
		this.methodName = methodName;
	}

	public ModelConverter(ModelCommonConverter commonconverter) {
		this(commonconverter.getMethodDeclaringType(), commonconverter.getMethodName());
	}

	public TypeElement getMethodDeclaringType() {
		return methodDeclaringType;
	}

	public String getMethodName() {
		return methodName;
	}

}
