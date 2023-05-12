package abstractfactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
class FactStreamConfiguration {
  @JsonProperty("in")
  FactStreamInboundConfiguration in;

  @JsonProperty("out")
  FactStreamOutboundConfiguration out;
}
