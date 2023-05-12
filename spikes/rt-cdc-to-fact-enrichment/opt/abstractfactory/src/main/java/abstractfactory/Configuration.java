package abstractfactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
// import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
class Configuration {
  @JsonProperty("facts")
  List<FactStreamConfiguration> facts;

  @SneakyThrows
  static Configuration fromResource(String path) {
    var cl = Configuration.class.getClassLoader();
    InputStream is = cl.getResourceAsStream(path);
    return Configuration.fromStream(is);
  }

  @SneakyThrows
  static Configuration fromFile(String path) {
    return Configuration.fromStream(new FileInputStream(path));
  }

  @SneakyThrows
  static Configuration fromStream(InputStream is) {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.findAndRegisterModules();
    return mapper.readValue(is, Configuration.class);
  }
}
