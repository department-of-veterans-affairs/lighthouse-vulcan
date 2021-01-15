package gov.va.api.lighthouse.vulcan.mappings;

import gov.va.api.lighthouse.vulcan.InvalidRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

@Value
@Builder
public class ReferenceParameter {
  private static final Collection<ReferenceFormat> SUPPORTED_FORMATS =
      List.of(
          new AbsoluteUrlFormat(),
          new RelativeUrlFormat(),
          new ResourceTypeAndValueFormat(),
          new ValueOnlyFormat());

  String parameterName;

  String value;

  String type;

  String publicId;

  String url;

  /** Create a ReferenceParameter from a reference search parameter. */
  public static ReferenceParameter parse(String parameterName, String parameterValue) {
    if (StringUtils.isBlank(parameterValue)) {
      throw InvalidRequest.noParametersSpecified();
    }
    List<String> help = new ArrayList<>();
    for (ReferenceFormat f : SUPPORTED_FORMATS) {
      var ref = f.tryParse(parameterName, parameterValue);
      if (ref != null) {
        return ref;
      }
      help.add(f.help());
    }
    throw InvalidRequest.because(
        String.format(
            "Reference parameter not parsable. Use one of the following formats: %s", help));
  }

  private interface ReferenceFormat {
    String help();

    ReferenceParameter tryParse(String parameterName, String parameterValue);
  }

  private static class AbsoluteUrlFormat implements ReferenceFormat {
    @Override
    public String help() {
      return "AbsoluteUrl format: "
          + "?param=http(s)://reference.com/ReferencePath/ReferenceResource/123";
    }

    @Override
    public ReferenceParameter tryParse(String parameterName, String parameterValue) {
      if (parameterValue.startsWith("http")) {
        try {
          URL url = new URL(parameterValue);
          var referenceParts = url.getPath().split("/", -1);
          if (referenceParts.length >= 2) {
            return ReferenceParameter.builder()
                .parameterName(parameterName)
                .value(parameterValue)
                .type(referenceParts[referenceParts.length - 2])
                .publicId(referenceParts[referenceParts.length - 1])
                .url(parameterValue)
                .build();
          } else {
            throw InvalidRequest.badParameter(
                parameterName, parameterValue, "Reference URL is not parsable.");
          }
        } catch (MalformedURLException e) {
          throw new IllegalStateException(
              String.format("Bad reference url: %s %s. %s", parameterName, parameterValue, e));
        }
      }
      return null;
    }
  }

  private static class RelativeUrlFormat implements ReferenceFormat {
    @Override
    public String help() {
      return "RelativeUrl format: ?parameter=ReferenceResource/id";
    }

    @Override
    public ReferenceParameter tryParse(String parameterName, String parameterValue) {
      if (parameterValue.matches("^[a-zA-Z]*/[a-zA-Z0-9]*$")) {
        var referenceParts = parameterValue.split("/", -1);
        return ReferenceParameter.builder()
            .parameterName(parameterName)
            .value(parameterValue)
            .type(referenceParts[0])
            .publicId(referenceParts[1])
            .build();
      }
      return null;
    }
  }

  private static class ResourceTypeAndValueFormat implements ReferenceFormat {
    @Override
    public String help() {
      return "ResourceTypeAndValue format: ?param:ReferencedResource=id";
    }

    @Override
    public ReferenceParameter tryParse(String parameterName, String parameterValue) {
      if (parameterName.matches("^[a-zA-Z-]*:[a-zA-Z]*$")) {
        var referenceParts = parameterName.split(":", -1);
        return ReferenceParameter.builder()
            .parameterName(parameterName)
            .value(parameterValue)
            .type(referenceParts[1])
            .publicId(parameterValue)
            .build();
      }
      return null;
    }
  }

  private static class ValueOnlyFormat implements ReferenceFormat {
    @Override
    public String help() {
      return null;
    }

    @Override
    public ReferenceParameter tryParse(String parameterName, String parameterValue) {
      return ReferenceParameter.builder()
          .parameterName(parameterName)
          .value(parameterValue)
          .type(parameterName.substring(0, 1).toUpperCase() + parameterName.substring(1))
          .publicId(parameterValue)
          .build();
    }
  }
}
