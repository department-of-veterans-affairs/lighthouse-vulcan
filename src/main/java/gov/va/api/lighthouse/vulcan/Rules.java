package gov.va.api.lighthouse.vulcan;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

/**
 * This provides some standard rules for HTTP request validation. If a rule fails and invalid
 * request exception will be thrown.
 */
@UtilityClass
public class Rules {

  /** Requires that at least on of the parameters be specified. */
  public Rule atLeastOneParameterOf(String... parameter) {
    return (ctx) -> {
      for (String p : parameter) {
        if (ctx.request().getParameter(p) != null) {
          return;
        }
      }
      throw InvalidRequest.because(
          "At least one of %s must be specified", Arrays.toString(parameter));
    };
  }

  /** Requires that all parameters be known by some mapping. */
  public Rule forbidUnknownParameters() {
    return (ctx) -> {
      var knownParameters =
          ctx.config().mappings().stream()
              .flatMap(m -> m.supportedParameterNames().stream())
              .collect(toSet());
      var unknownParameters =
          ctx.request().getParameterMap().keySet().stream()
              .filter(p -> !ctx.config().paging().isPagingRelatedParameter(p))
              .filter(p -> !knownParameters.contains(p))
              .collect(toList());
      if (!unknownParameters.isEmpty()) {
        throw InvalidRequest.because(
            "Unknown parameters %s, expecting %s", unknownParameters, knownParameters);
      }
    };
  }

  /** Requires that none of these parameters be specified. */
  public Rule forbiddenParameters(String... parameter) {
    return (ctx) -> {
      for (String p : parameter) {
        if (ctx.request().getParameter(p) != null) {
          throw InvalidRequest.because(
              "No parameter of %s can be specified", Arrays.toString(parameter));
        }
      }
    };
  }

  /**
   * Create a rule with conditional behavior to be specified if a given parameter is specified in
   * the request.
   */
  public IfParameterRuleBuilder ifParameter(String parameter) {
    return new IfParameterRuleBuilder(parameter);
  }

  /**
   * Create a rule that requires certain parameters to be specified together, e.g. latitude and
   * longitude.
   */
  public Rule parametersAlwaysSpecifiedTogether(String... parameter) {
    return (ctx) -> {
      int specified = 0;
      for (String p : parameter) {
        if (ctx.request().getParameter(p) != null) {
          specified++;
        }
      }
      if (specified > 0 && specified != parameter.length) {
        throw InvalidRequest.because(
            "Parameters %s must be specified together", Arrays.toString(parameter));
      }
    };
  }

  /** Create a rule that prevents parameters from being specified togethectx.request(). */
  public Rule parametersNeverSpecifiedTogether(String... parameter) {
    return (ctx) -> {
      int specified = 0;
      for (String p : parameter) {
        if (ctx.request().getParameter(p) != null) {
          specified++;
        }
      }
      if (specified > 0 && specified != 1) {
        throw InvalidRequest.because(
            "Parameters %s cannot be specified together", Arrays.toString(parameter));
      }
    };
  }

  @RequiredArgsConstructor
  public static class IfParameterRuleBuilder {
    private final String parameter;

    /** Require at least one of the given parameters to be specified. */
    public Rule thenAlsoAtLeastOneParameterOf(String... requiredParameters) {
      return (ctx) -> {
        if (isNotBlank(ctx.request().getParameter(parameter))) {
          atLeastOneParameterOf(requiredParameters).check(ctx);
        }
      };
    }

    /** Forbid all of the given parameters from being specified. */
    public Rule thenForbidParameters(String... forbiddenParameters) {
      return (ctx) -> {
        if (isNotBlank(ctx.request().getParameter(parameter))) {
          forbiddenParameters(forbiddenParameters).check(ctx);
        }
      };
    }
  }
}
