package sipka.cmdline.runtime;

public class MissingArgumentException extends ArgumentException {
	private static final long serialVersionUID = -1494335609710825928L;

	public MissingArgumentException(String message, String parameterName) {
		super(message, parameterName);
	}

	public MissingArgumentException(String parameterName) {
		super(parameterName);
	}

}
