package gov.va.api.lighthouse.vulcan.mappings;

import static org.apache.commons.lang3.StringUtils.isBlank;

import gov.va.api.lighthouse.vulcan.InvalidRequest;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;

@Value
@Builder
public class TokenParameter {
  String system;

  String code;

  @NonNull Mode mode;

  /** Create a TokenParameter from a token search parameter. */
  @SneakyThrows
  public static TokenParameter parse(String parameterName, String value) {
    if (isBlank(value) || value.equals("|")) {
      throw InvalidRequest.badParameter(
          parameterName, value, "Expected value, system|value, |value, or system|");
    }
    if (value.startsWith("|")) {
      return TokenParameter.builder()
          .code(value.substring(1))
          .mode(Mode.NO_SYSTEM_EXPLICIT_CODE)
          .build();
    }
    if (value.endsWith("|")) {
      return TokenParameter.builder()
          .system(value.substring(0, value.length() - 1))
          .mode(Mode.EXPLICIT_SYSTEM_ANY_CODE)
          .build();
    }
    if (value.contains("|")) {
      return TokenParameter.builder()
          .system(value.substring(0, value.indexOf("|")))
          .code(value.substring((value.indexOf("|") + 1)))
          .mode(Mode.EXPLICIT_SYSTEM_EXPLICIT_CODE)
          .build();
    }
    return TokenParameter.builder().code(value).mode(Mode.ANY_SYSTEM_EXPLICIT_CODE).build();
  }

  public BehaviorStemCell behavior() {
    return new BehaviorStemCell();
  }

  public boolean hasAnyCode() {
    return mode == Mode.EXPLICIT_SYSTEM_ANY_CODE;
  }

  public boolean hasAnySystem() {
    return mode == Mode.ANY_SYSTEM_EXPLICIT_CODE;
  }

  /** Determines if the token has an explicit code. */
  public boolean hasExplicitCode() {
    return mode == Mode.EXPLICIT_SYSTEM_EXPLICIT_CODE
        || mode == Mode.NO_SYSTEM_EXPLICIT_CODE
        || mode == Mode.ANY_SYSTEM_EXPLICIT_CODE;
  }

  public boolean hasExplicitSystem() {
    return mode == Mode.EXPLICIT_SYSTEM_ANY_CODE || mode == Mode.EXPLICIT_SYSTEM_EXPLICIT_CODE;
  }

  public boolean hasExplicitlyNoSystem() {
    return mode == Mode.NO_SYSTEM_EXPLICIT_CODE;
  }

  public <E extends Enum<E>> boolean hasSupportedCode(E supportedCode) {
    return supportedCode.toString().equals(code);
  }

  public boolean hasSupportedCode(String supportedCode) {
    return supportedCode.equals(code);
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public final <E extends Enum<E>> boolean hasSupportedCode(E... supportedCodes) {
    return Arrays.stream(supportedCodes).anyMatch(this::hasSupportedCode);
  }

  public boolean hasSupportedCode(String... supportedCodes) {
    return Arrays.stream(supportedCodes).anyMatch(this::hasSupportedCode);
  }

  public boolean hasSupportedSystem(String supportedSystem) {
    return supportedSystem.equals(system);
  }

  public boolean hasSupportedSystem(String... supportedSystems) {
    return Arrays.stream(supportedSystems).anyMatch(this::hasSupportedSystem);
  }

  public boolean isCodeExplicitAndUnsupported(String... supportedCodes) {
    return hasExplicitCode() && !hasSupportedCode(supportedCodes);
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public final <E extends Enum<E>> boolean isCodeExplicitAndUnsupported(E... supportedCodes) {
    return hasExplicitCode() && !hasSupportedCode(supportedCodes);
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public final <E extends Enum<E>> boolean isCodeExplicitlySetAndOneOf(E... supportedCodes) {
    return hasExplicitCode() && hasSupportedCode(supportedCodes);
  }

  public boolean isCodeExplicitlySetAndOneOf(String... supportedCodes) {
    return hasExplicitCode() && hasSupportedCode(supportedCodes);
  }

  public boolean isSystemExplicitAndUnsupported(String... supportedSystems) {
    return hasExplicitSystem() && !hasSupportedSystem(supportedSystems);
  }

  public boolean isSystemExplicitlySetAndOneOf(String... supportedSystems) {
    return hasExplicitSystem() && hasSupportedSystem(supportedSystems);
  }

  public enum Mode {
    /** e.g. cool */
    ANY_SYSTEM_EXPLICIT_CODE,
    /** e.g. http://fonzy.com| */
    EXPLICIT_SYSTEM_ANY_CODE,
    /** e.g. http://fonzy.com|cool */
    EXPLICIT_SYSTEM_EXPLICIT_CODE,
    /** e.g. |cool */
    NO_SYSTEM_EXPLICIT_CODE
  }

  @Value
  @Builder
  public static class Behavior<T> {
    @NonNull TokenParameter token;

    Function<String, T> onAnySystemAndExplicitCode;

    Function<String, T> onExplicitSystemAndAnyCode;

    BiFunction<String, String, T> onExplicitSystemAndExplicitCode;

    Function<String, T> onNoSystemAndExplicitCode;

    /** Check if behavior is specified before executing it. */
    public <T1> T1 check(T1 n) {
      if (n == null) {
        throw new IllegalStateException("no handler specified for " + token().mode);
      }
      return n;
    }

    /** Execute correct behavior based on the mode of the token. */
    @SuppressWarnings("UnnecessaryDefault")
    @SneakyThrows
    public T execute() {
      switch (token().mode) {
        case ANY_SYSTEM_EXPLICIT_CODE:
          return check(onAnySystemAndExplicitCode()).apply(token().code);
        case EXPLICIT_SYSTEM_ANY_CODE:
          return check(onExplicitSystemAndAnyCode()).apply(token().system);
        case EXPLICIT_SYSTEM_EXPLICIT_CODE:
          return check(onExplicitSystemAndExplicitCode()).apply(token().system, token().code);
        case NO_SYSTEM_EXPLICIT_CODE:
          return check(onNoSystemAndExplicitCode()).apply(token().code);
        default:
          throw new IllegalStateException("TokenParameter in unsupported mode : " + token().mode);
      }
    }
  }

  public final class BehaviorStemCell {
    public <T> Behavior.BehaviorBuilder<T> onAnySystemAndExplicitCode(Function<String, T> f) {
      return Behavior.<T>builder().token(TokenParameter.this).onAnySystemAndExplicitCode(f);
    }

    public <T> Behavior.BehaviorBuilder<T> onExplicitSystemAndAnyCode(Function<String, T> f) {
      return Behavior.<T>builder().token(TokenParameter.this).onExplicitSystemAndAnyCode(f);
    }

    public <T> Behavior.BehaviorBuilder<T> onExplicitSystemAndExplicitCode(
        BiFunction<String, String, T> f) {
      return Behavior.<T>builder().token(TokenParameter.this).onExplicitSystemAndExplicitCode(f);
    }

    @SuppressWarnings("unused")
    public <T> Behavior.BehaviorBuilder<T> onNoSystemAndExplicitCode(Function<String, T> f) {
      return Behavior.<T>builder().token(TokenParameter.this).onNoSystemAndExplicitCode(f);
    }
  }
}
