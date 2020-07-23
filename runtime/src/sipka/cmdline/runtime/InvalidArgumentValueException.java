package sipka.cmdline.runtime;

public class InvalidArgumentValueException extends ArgumentException {
	private static final long serialVersionUID = -4528508936632888463L;

	public InvalidArgumentValueException(String message, String parameterName) {
		super(message, parameterName);
	}

	public InvalidArgumentValueException(String message, Throwable cause, String parameterName) {
		super(message, cause, parameterName);
	}

	public InvalidArgumentValueException(String parameterName) {
		super(parameterName);
	}

	public InvalidArgumentValueException(Throwable cause, String parameterName) {
		super(cause, parameterName);
	}
}
