package sipka.cmdline.runtime;

public class ArgumentResolutionException extends ArgumentException {
	private static final long serialVersionUID = -6489439183279544507L;

	public ArgumentResolutionException(String message, String parameterName) {
		super(message, parameterName);
	}

	public ArgumentResolutionException(String message, Throwable cause, String parameterName) {
		super(message, cause, parameterName);
	}

	public ArgumentResolutionException(String parameterName) {
		super(parameterName);
	}

	public ArgumentResolutionException(Throwable cause, String parameterName) {
		super(cause, parameterName);
	}
}
