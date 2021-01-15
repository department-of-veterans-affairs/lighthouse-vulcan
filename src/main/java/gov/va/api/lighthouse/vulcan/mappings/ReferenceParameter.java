package gov.va.api.lighthouse.vulcan.mappings;

import gov.va.api.lighthouse.vulcan.InvalidRequest;
import java.net.MalformedURLException;
import java.net.URL;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReferenceParameter {
  String parameterName;
  String value;
  String type;
  String publicId;
  String url;

  /** Create a ReferenceParameter from a reference search parameter. */
  public static ReferenceParameter parse(String parameterName, String parameterValue) {
    /*
     xxx=https://good.com/Patient/123
     xxx=https://good.com/fhir/v0/r4/Patient?name=123 -> allow through as valid url, but isSupported will explode on type=r4
     xxx=123.456 -> falls through to default parse
    */
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
      } catch (MalformedURLException ignored) {
        // Do nothing if we can't parse the URL, test for other reference structures
      }
    }
    // xxx:Patient=123
    if (parameterName.matches("^[a-zA-Z-]*:[a-zA-Z]*$")) {
      var referenceParts = parameterName.split(":", -1);
      return ReferenceParameter.builder()
          .parameterName(referenceParts[0])
          .value(parameterValue)
          .type(referenceParts[1])
          .publicId(parameterValue)
          .build();
    }
    // ?subject=Patient/123
    if (parameterValue.matches("^[a-zA-Z]*/[a-zA-Z0-9]*$")) {
      var referenceParts = parameterValue.split("/", -1);
      return ReferenceParameter.builder()
          .parameterName(parameterName)
          .value(parameterValue)
          .type(referenceParts[0])
          .publicId(referenceParts[1])
          .build();
    }
    // Condition?patient=123
    return ReferenceParameter.builder()
        .parameterName(parameterName)
        .value(parameterValue)
        .type(parameterName)
        .publicId(parameterValue)
        .build();
  }
}
