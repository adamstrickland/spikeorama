package abstractfactory;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

@Slf4j
public class FactFactory {
  public static void main(String[] args) throws Exception {
    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    final String bootstrapServers = System.getenv("KAFKA_URL");

    TransformJob job = args.length > 0 && args[0].equals("configured")
        ? new ConfiguredTransformJob()
        : new StaticTransformJob();

    job.configure(env, bootstrapServers);

    env.execute("Fact Factory");
  }
}
