package gov.va.api.lighthouse.vulcan;

import javax.servlet.http.HttpServletRequest;

/** Provides context for rule evaluation. */
public interface RuleContext {
  VulcanConfiguration<?> config();

  HttpServletRequest request();
}
