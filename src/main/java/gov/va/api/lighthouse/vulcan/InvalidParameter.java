package gov.va.api.lighthouse.vulcan;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Indicate there parameter is not valid. This would indicate that a 404 Bad Request should be
 * returned to the user.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class InvalidParameter extends IllegalArgumentException {
  public InvalidParameter(String message) {
    super(message);
  }

  /** Create a new exception for bad value, like a number that cannot be parsed. */
  public static InvalidParameter badValue(String parameter, String value, String message) {
    return new InvalidParameter(String.format("%s = %s : %s", parameter, value, message));
  }

  /** Create a new exception for a parameter that has been repeated to much. */
  public static InvalidParameter repeatedTooManyTimes(String parameter, int max, int actual) {
    return new InvalidParameter(
        String.format(
            "%s specified too many %d times, up to %d is allowed", parameter, actual, max));
  }
}
