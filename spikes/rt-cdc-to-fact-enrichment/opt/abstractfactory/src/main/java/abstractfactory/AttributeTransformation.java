package abstractfactory;

import java.io.Serializable;

public interface AttributeTransformation<O extends Serializable> {
  O transform(Object in);
}
