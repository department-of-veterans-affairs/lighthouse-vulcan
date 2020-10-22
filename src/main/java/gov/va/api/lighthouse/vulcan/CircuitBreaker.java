package gov.va.api.lighthouse.vulcan;

/**
 * This exception can be thrown by Mapping instance when attempting to build specifications to short
 * circuit searching the database. This exception means that we already know, even before searching
 * the database that no results will be found.
 */
public class CircuitBreaker extends RuntimeException {

  public CircuitBreaker(String message) {
    super(message);
  }

  /** Create a new exception for bad value, like a number that cannot be parsed. */
  public static CircuitBreaker noResultsWillBeFound(
      String parameter, String value, String message) {
    return new CircuitBreaker(
        String.format("No results will be found for %s = %s : %s", parameter, value, message));
  }
}
