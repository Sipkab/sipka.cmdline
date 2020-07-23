package sipka.cmdline.runtime;

public class InvalidArgumentFormatException extends ArgumentException {
	private static final long serialVersionUID = 4842803806968639273L;

	public InvalidArgumentFormatException(String message, String parameterName) {
		super(message, parameterName);
	}

	public InvalidArgumentFormatException(String message, Throwable cause, String parameterName) {
		super(message, cause, parameterName);
	}

	public InvalidArgumentFormatException(String parameterName) {
		super(parameterName);
	}

	public InvalidArgumentFormatException(Throwable cause, String parameterName) {
		super(cause, parameterName);
	}

}
