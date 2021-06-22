package gov.va.api.lighthouse.vulcan;

import static java.lang.String.join;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.UtilityClass;

/**
 * This provides some standard rules for HTTP request validation. If a rule fails and invalid
 * request exception will be thrown. Rules are aware of FHIR style modifiers (parameter:modifier),
 * e.g. string parameters have `name:exact` or `name:contains` parameter name modifier.
 */
@UtilityClass
public class Rules {
  /** Requires that at least on of the parameters be specified. */
  public Rule atLeastOneParameterOf(String... parameter) {
    return (ctx) -> {
      var specifiedParameters = ctx.request().getParameterMap().keySet();
      for (String p : parameter) {
        if (isParameterOrModifiedParameterSpecified(specifiedParameters, p)) {
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
      var knownParameters = ctx.config().supportedParameters();
      var unknownParameters =
          ctx.request().getParameterMap().keySet().stream()
              .filter(p -> !ctx.config().paging().isPagingRelatedParameter(p))
              .filter(p -> !knownParameters.contains(p))
              .filter(p -> !isModifiedParameter(p))
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
      var specifiedParameters = ctx.request().getParameterMap().keySet();
      for (String p : parameter) {
        if (specifiedParameters.contains(p)) {
          throw InvalidRequest.because(
              "No parameter of %s can be specified", Arrays.toString(parameter));
        }
        var modified = specifiedParameters.stream().filter(isModifiedVersionOf(p)).findFirst();
        if (modified.isPresent()) {
          throw InvalidRequest.because(
              "No parameter of %s can be specified. Found modified parameter %s",
              Arrays.toString(parameter), modified.get());
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

  private boolean isModifiedParameter(String parameter) {
    return parameter.indexOf(':') > 0;
  }

  private Predicate<String> isModifiedVersionOf(String baseParameterName) {
    return parameter -> parameter.startsWith(baseParameterName + ":");
  }

  private boolean isParameterOrModifiedParameterSpecified(
      Set<String> specifiedParameters, String parameter) {
    if (specifiedParameters.contains(parameter)) {
      return true;
    }
    return specifiedParameters.stream().anyMatch(isModifiedVersionOf(parameter));
  }

  /**
   * Create a rule that requires certain parameters to be specified together, e.g. latitude and
   * longitude.
   */
  public Rule parametersAlwaysSpecifiedTogether(String... parameter) {
    return (ctx) -> {
      int specified = 0;
      var specifiedParameters = ctx.request().getParameterMap().keySet();
      for (String p : parameter) {
        if (isParameterOrModifiedParameterSpecified(specifiedParameters, p)) {
          specified++;
        }
      }
      if (specified > 0 && specified != parameter.length) {
        throw InvalidRequest.because(
            "Parameters %s must be specified together", Arrays.toString(parameter));
      }
    };
  }

  /** Create a rule that prevents parameters from being specified together. */
  public Rule parametersNeverSpecifiedTogether(String... parameter) {
    return (ctx) -> {
      int specified = 0;
      var specifiedParameters = ctx.request().getParameterMap().keySet();
      for (String p : parameter) {
        if (isParameterOrModifiedParameterSpecified(specifiedParameters, p)) {
          specified++;
        }
      }
      if (specified > 0 && specified != 1) {
        throw InvalidRequest.because(
            "Parameters %s cannot be specified together", Arrays.toString(parameter));
      }
    };
  }

  @Value
  @RequiredArgsConstructor
  public static class IfParameterRuleBuilder {
    String parameter;

    /**
     * Forbid any unknown parameter modifiers. Known modifiers are determined both by the mappings
     * themselves, but also any provided to the method.
     */
    public Rule thenAllowOnlyKnownModifiers(String... additionalSupportedModifiers) {
      return (ctx) -> {
        // If parameter has no modifier, continue
        if (ctx.request().getParameter(parameter()) != null) {
          return;
        }
        var supportedParameters =
            ctx.config().supportedParameters().stream()
                .filter(p -> p.startsWith(parameter()))
                .filter(p -> p.contains(":"))
                .collect(toSet());
        var allowedParameters =
            Stream.concat(
                    supportedParameters.stream(),
                    Arrays.stream(additionalSupportedModifiers).map(m -> join(":", parameter(), m)))
                .collect(toSet());
        ctx.request().getParameterMap().keySet().stream()
            .filter(p -> p.startsWith(parameter()))
            .forEach(
                p -> {
                  if (!allowedParameters.contains(p)) {
                    throw InvalidRequest.badParameter(
                        p, ctx.request().getParameter(p), "Modifier not allowed.");
                  }
                });
      };
    }

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
