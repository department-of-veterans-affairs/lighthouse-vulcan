package gov.va.api.lighthouse.vulcan;

/**
 * Instances are applied to request object and have the opportunity to reject the request by
 * throwing an instance of InvalidRequest exception.
 */
@FunctionalInterface
public interface Rule {

  /**
   * Check some aspect of the request and thrown an InvalidRequest exception if some condition is
   * not satisfied, e.g., a required parameter is not set.
   */
  void check(RuleContext context);
}
