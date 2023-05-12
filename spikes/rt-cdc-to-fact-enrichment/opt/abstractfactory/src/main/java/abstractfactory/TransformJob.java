package abstractfactory;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public interface TransformJob {
  void configure(StreamExecutionEnvironment envs, String bootstrapServers);
}
