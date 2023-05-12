package abstractfactory;

import java.io.Serializable;

public class StringToStringAttributeTransformation implements AttributeTransformation<String>{
  @Override
  public String transform(Object in) {
    return in.toString();
  }
}
