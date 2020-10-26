package gov.va.api.lighthouse.vulcan.mappings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gov.va.api.lighthouse.vulcan.InvalidRequest;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TokenParameterTest {
  TokenParameter noSystemExplicitCodeToken =
      TokenParameter.builder()
          .code("code")
          .system(null)
          .mode(TokenParameter.Mode.NO_SYSTEM_EXPLICIT_CODE)
          .build();

  TokenParameter explicitSystemAnyCodeToken =
      TokenParameter.builder()
          .code(null)
          .system("system")
          .mode(TokenParameter.Mode.EXPLICIT_SYSTEM_ANY_CODE)
          .build();

  TokenParameter explicitSystemExplicitCodeToken =
      TokenParameter.builder()
          .code("code")
          .system("system")
          .mode(TokenParameter.Mode.EXPLICIT_SYSTEM_EXPLICIT_CODE)
          .build();

  TokenParameter anySystemExplicitCodeToken =
      TokenParameter.builder()
          .code("code")
          .system(null)
          .mode(TokenParameter.Mode.ANY_SYSTEM_EXPLICIT_CODE)
          .build();

  Function<String, String> anySystemAndExplicitCode = c -> "c is for " + c;

  Function<String, String> explicitSystemAndAnyCode = s -> "s is for " + s;

  Function<String, String> noSystemAndExplicitCode = c -> "c is for " + c;

  BiFunction<String, String, String> explicitSystemAndExplicitCode =
      (s, c) -> "s is for " + s + ", c is for " + c;

  @Test
  public void booleanSupport() {
    assertThat(noSystemExplicitCodeToken.hasAnySystem()).isFalse();
    assertThat(noSystemExplicitCodeToken.hasExplicitSystem()).isFalse();
    assertThat(noSystemExplicitCodeToken.hasSupportedSystem("system")).isFalse();
    assertThat(noSystemExplicitCodeToken.hasExplicitlyNoSystem()).isTrue();
    assertThat(noSystemExplicitCodeToken.hasAnyCode()).isFalse();
    assertThat(noSystemExplicitCodeToken.hasExplicitCode()).isTrue();
    assertThat(noSystemExplicitCodeToken.hasSupportedCode("code")).isTrue();
    assertThat(noSystemExplicitCodeToken.hasSupportedSystem("system")).isFalse();
    assertThat(noSystemExplicitCodeToken.isSystemExplicitAndUnsupported("system")).isFalse();
    assertThat(noSystemExplicitCodeToken.isCodeExplicitAndUnsupported("code")).isFalse();
    assertThat(noSystemExplicitCodeToken.isCodeExplicitlySetAndOneOf(Codes.code)).isTrue();
    assertThat(noSystemExplicitCodeToken.isCodeExplicitlySetAndOneOf("code", "other"));
    assertThat(noSystemExplicitCodeToken.isCodeExplicitlySetAndOneOf("nope", "other")).isFalse();
    assertThat(explicitSystemExplicitCodeToken.hasAnySystem()).isFalse();
    assertThat(explicitSystemExplicitCodeToken.hasExplicitSystem()).isTrue();
    assertThat(explicitSystemExplicitCodeToken.hasSupportedSystem("system")).isTrue();
    assertThat(explicitSystemExplicitCodeToken.hasSupportedSystem("notsystem")).isFalse();
    assertThat(explicitSystemExplicitCodeToken.hasExplicitlyNoSystem()).isFalse();
    assertThat(explicitSystemExplicitCodeToken.hasAnyCode()).isFalse();
    assertThat(explicitSystemExplicitCodeToken.hasExplicitCode()).isTrue();
    assertThat(explicitSystemExplicitCodeToken.hasSupportedCode(("code"))).isTrue();
    assertThat(explicitSystemExplicitCodeToken.hasSupportedSystem("system")).isTrue();
    assertThat(explicitSystemExplicitCodeToken.isSystemExplicitAndUnsupported(("system")))
        .isFalse();
    assertThat(explicitSystemExplicitCodeToken.isCodeExplicitAndUnsupported("code")).isFalse();
    assertThat(explicitSystemExplicitCodeToken.isSystemExplicitlySetAndOneOf("system", "other"))
        .isTrue();
    assertThat(explicitSystemExplicitCodeToken.isSystemExplicitlySetAndOneOf("nope", "other"))
        .isFalse();
    assertThat(anySystemExplicitCodeToken.hasAnySystem()).isTrue();
    assertThat(anySystemExplicitCodeToken.hasExplicitSystem()).isFalse();
    assertThat(anySystemExplicitCodeToken.hasSupportedSystem("system")).isFalse();
    assertThat(anySystemExplicitCodeToken.hasExplicitlyNoSystem()).isFalse();
    assertThat(anySystemExplicitCodeToken.hasAnyCode()).isFalse();
    assertThat(anySystemExplicitCodeToken.hasExplicitCode()).isTrue();
    assertThat(anySystemExplicitCodeToken.hasSupportedCode("code")).isTrue();
    assertThat(noSystemExplicitCodeToken.hasSupportedSystem("system")).isFalse();
    assertThat(anySystemExplicitCodeToken.isSystemExplicitAndUnsupported("notsystem")).isFalse();
    assertThat(noSystemExplicitCodeToken.isCodeExplicitAndUnsupported("notcode")).isTrue();
    assertThat(explicitSystemAnyCodeToken.hasAnySystem()).isFalse();
    assertThat(explicitSystemAnyCodeToken.hasExplicitSystem()).isTrue();
    assertThat(explicitSystemAnyCodeToken.hasSupportedSystem("system")).isTrue();
    assertThat(explicitSystemAnyCodeToken.hasSupportedSystem("notsystem")).isFalse();
    assertThat(explicitSystemAnyCodeToken.hasExplicitlyNoSystem()).isFalse();
    assertThat(explicitSystemAnyCodeToken.hasAnyCode()).isTrue();
    assertThat(explicitSystemAnyCodeToken.hasExplicitCode()).isFalse();
    assertThat(explicitSystemAnyCodeToken.hasSupportedCode("code")).isFalse();
    assertThat(explicitSystemAnyCodeToken.hasSupportedSystem("system")).isTrue();
    assertThat(explicitSystemAnyCodeToken.isSystemExplicitAndUnsupported("notsystem")).isTrue();
    assertThat(explicitSystemAnyCodeToken.isCodeExplicitAndUnsupported("notcode")).isFalse();
  }

  @Test
  public void checkNullThrowsIllegalState() {
    assertThrows(
        IllegalStateException.class,
        () ->
            anySystemExplicitCodeToken
                .behavior()
                .onAnySystemAndExplicitCode(null)
                .onExplicitSystemAndAnyCode(null)
                .onExplicitSystemAndExplicitCode(null)
                .onNoSystemAndExplicitCode(null)
                .build()
                .execute());
  }

  @Test
  public void execute() {
    assertThat(
            anySystemExplicitCodeToken
                .behavior()
                .onAnySystemAndExplicitCode(anySystemAndExplicitCode)
                .onExplicitSystemAndAnyCode(explicitSystemAndAnyCode)
                .onExplicitSystemAndExplicitCode(explicitSystemAndExplicitCode)
                .onNoSystemAndExplicitCode(noSystemAndExplicitCode)
                .build()
                .execute())
        .isEqualTo("c is for code");
    assertThat(
            explicitSystemExplicitCodeToken
                .behavior()
                .onExplicitSystemAndAnyCode(explicitSystemAndAnyCode)
                .onAnySystemAndExplicitCode(anySystemAndExplicitCode)
                .onExplicitSystemAndExplicitCode(explicitSystemAndExplicitCode)
                .onNoSystemAndExplicitCode(noSystemAndExplicitCode)
                .build()
                .execute())
        .isEqualTo("s is for system, c is for code");
    assertThat(
            explicitSystemAnyCodeToken
                .behavior()
                .onExplicitSystemAndExplicitCode(explicitSystemAndExplicitCode)
                .onAnySystemAndExplicitCode(anySystemAndExplicitCode)
                .onExplicitSystemAndAnyCode(explicitSystemAndAnyCode)
                .onNoSystemAndExplicitCode(noSystemAndExplicitCode)
                .build()
                .execute())
        .isEqualTo("s is for system");
    assertThat(
            noSystemExplicitCodeToken
                .behavior()
                .onAnySystemAndExplicitCode(anySystemAndExplicitCode)
                .onExplicitSystemAndAnyCode(explicitSystemAndAnyCode)
                .onExplicitSystemAndExplicitCode(explicitSystemAndExplicitCode)
                .onNoSystemAndExplicitCode(noSystemAndExplicitCode)
                .build()
                .execute())
        .isEqualTo("c is for code");
  }

  @Test
  public void parseBlank() {
    assertThrows(InvalidRequest.class, () -> TokenParameter.parse("x", ""));
  }

  @Test
  public void parseNull() {
    assertThrows(InvalidRequest.class, () -> TokenParameter.parse("x", null));
  }

  @Test
  public void parsePipe() {
    assertThrows(InvalidRequest.class, () -> TokenParameter.parse("x", "|"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"code", "|code", "system|code", "system|"})
  public void throwsExceptionWhenBehaviorIsNotConfigured(String value) {
    var token = TokenParameter.parse("x", value);
    var behavior =
        token
            .behavior()
            .onNoSystemAndExplicitCode(null)
            .onExplicitSystemAndExplicitCode(null)
            .onExplicitSystemAndAnyCode(null)
            .onAnySystemAndExplicitCode(null)
            .build();
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> behavior.execute());
  }

  @Test
  public void validParse() {
    assertThat(TokenParameter.parse("x", "|code")).isEqualTo(noSystemExplicitCodeToken);
    assertThat(TokenParameter.parse("x", "system|")).isEqualTo(explicitSystemAnyCodeToken);
    assertThat(TokenParameter.parse("x", "system|code")).isEqualTo(explicitSystemExplicitCodeToken);
    assertThat(TokenParameter.parse("x", "code")).isEqualTo(anySystemExplicitCodeToken);
  }

  enum Codes {
    code;
  }
}
