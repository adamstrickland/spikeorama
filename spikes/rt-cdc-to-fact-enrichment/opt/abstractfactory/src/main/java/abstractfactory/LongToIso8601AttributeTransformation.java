package abstractfactory;

import java.io.Serializable;
import java.time.Instant;

public class LongToIso8601AttributeTransformation implements AttributeTransformation<String> {
  @Override
  public String transform(Object in) {
    Instant inst = Instant.ofEpochMilli((Long)in);
    return inst.toString();
  }
}
