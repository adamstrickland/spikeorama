package abstractfactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;
import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor
class FactStreamOutboundAttributeConfiguration {
  @JsonProperty("name")
  String name;

  @JsonProperty("expr")
  String expr;

  @JsonProperty("tfrm")
  Optional<String> tfrm;

  @JsonProperty("type")
  Optional<String> type;



  Class<? extends AttributeTransformation<String>> getTransformer() {
    if (this.tfrm.isPresent() && this.tfrm.get().equals("LongToIso8601AttributeTransformation")) {
      return LongToIso8601AttributeTransformation.class;
    } else {
      return StringToStringAttributeTransformation.class;
    }
  }

  Class<? extends Serializable> getType() {
    if (this.type.isPresent() && this.type.get().equals("Long")) {
      return Long.class;
    } else {
      return String.class;
    }
  }
}
