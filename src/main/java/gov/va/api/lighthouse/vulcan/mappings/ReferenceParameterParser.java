package gov.va.api.lighthouse.vulcan.mappings;

import gov.va.api.lighthouse.vulcan.InvalidRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

@Builder
public class ReferenceParameterParser {

  private final Collection<ReferenceFormat> supportedFormats =
      List.of(
          new AbsoluteUrlFormat(),
          new RelativeUrlFormat(),
          new ResourceTypeAndValueFormat(),
          new ValueOnlyFormat());

  private final String parameterName;
  private final String parameterValue;
  private final Set<String> allowedReferenceTypes;
  private final String defaultResourceType;

  /** Create a ReferenceParameter from a reference search parameter. */
  public ReferenceParameter parse() {
    if (StringUtils.isBlank(parameterValue)) {
      throw InvalidRequest.noParametersSpecified();
    }
    List<String> help = new ArrayList<>();
    for (ReferenceFormat f : supportedFormats) {
      var ref = f.tryParse();
      if (ref != null) {
        return ref;
      }
      help.add(f.help());
    }
    throw InvalidRequest.because(
        String.format(
            "Reference parameter not parsable. Use one of the following formats: %s", help));
  }

  interface ReferenceFormat {
    String help();

    ReferenceParameter tryParse();
  }

  class AbsoluteUrlFormat implements ReferenceFormat {
    @Override
    public String help() {
      return "AbsoluteUrl format: "
          + "?param=http(s)://reference.com/ReferencePath/ReferenceResource/123";
    }

    @Override
    public ReferenceParameter tryParse() {
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

  class RelativeUrlFormat implements ReferenceFormat {
    @Override
    public String help() {
      return "RelativeUrl format: ?parameter=ReferenceResource/id";
    }

    @Override
    public ReferenceParameter tryParse() {
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

  class ResourceTypeAndValueFormat implements ReferenceFormat {
    @Override
    public String help() {
      return "ResourceTypeAndValue format: ?param:ReferencedResource=id";
    }

    @Override
    public ReferenceParameter tryParse() {
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

  class ValueOnlyFormat implements ReferenceFormat {
    @Override
    public String help() {
      return "ValueOnly format: ?resource=id";
    }

    @Override
    public ReferenceParameter tryParse() {
      String resourceType;
      if (allowedReferenceTypes.contains(parameterName) && allowedReferenceTypes.size() == 1) {
        resourceType = parameterName;
      } else {
        resourceType = defaultResourceType;
      }
      return ReferenceParameter.builder()
          .parameterName(parameterName)
          .value(parameterValue)
          .type(resourceType)
          .publicId(parameterValue)
          .build();
    }
  }
}
