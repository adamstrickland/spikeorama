package abstractfactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
@AllArgsConstructor
@NoArgsConstructor
class FactStreamOutboundConfiguration {
  @JsonProperty("topic")
  String topic;

  @JsonProperty("type")
  String type;

  @JsonProperty("attributes")
  List<FactStreamOutboundAttributeConfiguration> attributes;
}
