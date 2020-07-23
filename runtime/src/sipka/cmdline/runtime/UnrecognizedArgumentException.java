package sipka.cmdline.runtime;

public class UnrecognizedArgumentException extends ArgumentException {
	private static final long serialVersionUID = 3188122669083575224L;

	public UnrecognizedArgumentException(String message, String parameterName) {
		super(message, parameterName);
	}

	public UnrecognizedArgumentException(String message, Throwable cause, String parameterName) {
		super(message, cause, parameterName);
	}

	public UnrecognizedArgumentException(String parameterName) {
		super(parameterName);
	}

	public UnrecognizedArgumentException(Throwable cause, String parameterName) {
		super(cause, parameterName);
	}

}
