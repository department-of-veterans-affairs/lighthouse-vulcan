package gov.va.api.lighthouse.vulcan;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Indicate there parameter is not valid. This would indicate that a 404 Bad Request should be
 * returned to the user.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class InvalidRequest extends IllegalArgumentException {
  public InvalidRequest(String message) {
    super(message);
  }

  /** Create a new exception for bad value, like a number that cannot be parsed. */
  public static InvalidRequest badParameter(String parameter, String value, String message) {
    return because("bad parameter: %s = %s : %s", parameter, value, message);
  }

  public static InvalidRequest because(String message) {
    return new InvalidRequest(message);
  }

  @SuppressWarnings("AnnotateFormatMethod")
  public static InvalidRequest because(String format, Object... messageValues) {
    return new InvalidRequest(String.format(format, messageValues));
  }

  /** Create a new exception for a parameter that has been repeated to much. */
  public static InvalidRequest noParametersSpecified() {
    return because("No parameters specified.");
  }

  /** Create a new exception for a parameter that has been repeated to much. */
  public static InvalidRequest repeatedTooManyTimes(String parameter, int max, int actual) {
    return because("%s specified too many %d times, up to %d is allowed", parameter, actual, max);
  }
}
