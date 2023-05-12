package abstractfactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
class FactStreamInboundConfiguration {
  @JsonProperty("topic")
  String topic;
}
