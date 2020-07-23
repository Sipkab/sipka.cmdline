package sipka.cmdline.runtime;

/**
 * General exception superclass for argument related errors.
 */
public class ArgumentException extends IllegalArgumentException {
	private static final long serialVersionUID = 789250634061124442L;

	private String parameterName;

	public ArgumentException(String parameterName) {
		this.parameterName = parameterName;
	}

	public ArgumentException(Throwable cause, String parameterName) {
		super(cause);
		this.parameterName = parameterName;
	}

	public ArgumentException(String message, Throwable cause, String parameterName) {
		super(message, cause);
		this.parameterName = parameterName;
	}

	public ArgumentException(String message, String parameterName) {
		super(message);
		this.parameterName = parameterName;
	}

	public final String getParameterName() {
		return parameterName;
	}

	@Override
	public String toString() {
		String cname = getClass().getName();
		String msg = getLocalizedMessage();
		StringBuilder sb = new StringBuilder();
		sb.append(cname);
		sb.append(": ");
		sb.append(parameterName);
		if (msg != null) {
			sb.append(": ");
			sb.append(msg);
		}
		return sb.toString();
	}
}
