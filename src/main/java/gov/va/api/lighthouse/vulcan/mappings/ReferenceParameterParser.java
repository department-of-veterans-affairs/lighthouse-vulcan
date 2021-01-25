package gov.va.api.lighthouse.vulcan.mappings;

import static org.apache.commons.lang3.StringUtils.isBlank;

import gov.va.api.lighthouse.vulcan.InvalidRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class ReferenceParameterParser {

  private final String parameterName;
  private final String parameterValue;
  private final Set<String> allowedReferenceTypes;
  @NonNull private final Collection<ReferenceFormat> formats;

  /** Get a set of standard formatters for a fhir resource reference. */
  public static Collection<ReferenceFormat> standardFormatsForResource(
      String resourceType, Set<String> allowedReferenceTypes) {
    return List.of(
        new ValueOnlyFormat(resourceType, allowedReferenceTypes),
        new TypeModifierAndValueFormat(),
        new RelativeUrlFormat(),
        new AbsoluteUrlFormat());
  }

  /** Create a ReferenceParameter from a reference search parameter. */
  public ReferenceParameter parse() {
    if (isBlank(parameterName) || isBlank(parameterValue)) {
      throw new InvalidRequest(
          String.format(
              "Cannot parse, missing parameter. Parameter name: %s, Parameter value: %s",
              parameterName, parameterValue));
    }
    List<String> help = new ArrayList<>();
    for (ReferenceFormat f : formats) {
      var ref = f.tryParse(parameterName, parameterValue);
      if (ref == null) {
        help.add(f.help());
        continue;
      }
      if (!allowedReferenceTypes.contains(ref.type())) {
        throw InvalidRequest.because(
            String.format(
                "ReferenceParameter type [%s] is not legal as per the spec. "
                    + "Allowed types are: %s",
                ref.type(), allowedReferenceTypes));
      }
      return ref;
    }
    throw InvalidRequest.because(
        String.format(
            "Reference parameter not parsable. Use one of the following formats: %s", help));
  }

  interface ReferenceFormat {
    String help();

    ReferenceParameter tryParse(String parameterName, String value);
  }

  static class AbsoluteUrlFormat implements ReferenceFormat {
    @Override
    public String help() {
      return "parameter=http(s)://url.com/path/ResourceType/id";
    }

    @Override
    public ReferenceParameter tryParse(String parameterName, String value) {
      if (value.startsWith("http")) {
        try {
          URL url = new URL(value);
          var referenceParts = url.getPath().split("/", -1);
          if (referenceParts.length >= 2) {
            return ReferenceParameter.builder()
                .parameterName(parameterName)
                .value(value)
                .type(referenceParts[referenceParts.length - 2])
                .publicId(referenceParts[referenceParts.length - 1])
                .url(Optional.of(value))
                .build();
          } else {
            throw InvalidRequest.badParameter(
                parameterName,
                value,
                "Absolute reference URL is not parsable. "
                    + "URLs must be of a format: basePath/Resource/id");
          }
        } catch (MalformedURLException e) {
          throw new IllegalStateException(
              String.format("Bad reference url: %s %s. %s", parameterName, value, e));
        }
      }
      return null;
    }
  }

  static class RelativeUrlFormat implements ReferenceFormat {
    @Override
    public String help() {
      return "parameter=ResourceType/id";
    }

    @Override
    public ReferenceParameter tryParse(String parameterName, String value) {
      if (value.matches("^[a-zA-Z]*/[A-Za-z0-9-.]{1,64}$")) {
        var referenceParts = value.split("/", -1);
        return ReferenceParameter.builder()
            .parameterName(parameterName)
            .value(value)
            .type(referenceParts[0])
            .publicId(referenceParts[1])
            .build();
      }
      return null;
    }
  }

  static class TypeModifierAndValueFormat implements ReferenceFormat {
    @Override
    public String help() {
      return "parameter:ResourceType=id";
    }

    @Override
    public ReferenceParameter tryParse(String parameterName, String value) {
      if (parameterName.matches("^[a-zA-Z-]*:[A-Za-z0-9-.]{1,64}$")) {
        var referenceParts = parameterName.split(":", -1);
        return ReferenceParameter.builder()
            .parameterName(parameterName)
            .value(value)
            .type(referenceParts[1])
            .publicId(value)
            .build();
      }
      return null;
    }
  }

  @AllArgsConstructor
  static class ValueOnlyFormat implements ReferenceFormat {

    @NonNull String defaultResourceType;
    @NonNull Set<String> allowedReferenceTypes;

    @Override
    public String help() {
      return "parameter=id, Legal ids are... (^[A-Za-z0-9-.]{1,64}$)";
    }

    @Override
    public ReferenceParameter tryParse(String parameterName, String value) {
      if (parameterName.matches("^[A-Za-z0-9-]*") && value.matches("^[A-Za-z0-9-.]{1,64}$")) {
        if (allowedReferenceTypes.size() > 1) {
          throw InvalidRequest.badParameter(
              parameterName,
              value,
              "Cannot search by value on a reference that allows more than 1 type."
                  + " To do so explicitly use the type modifier..."
                  + " parameter:resource=id ");
        }
        return ReferenceParameter.builder()
            .parameterName(parameterName)
            .value(value)
            .type(defaultResourceType)
            .publicId(value)
            .build();
      }
      return null;
    }
  }
}
