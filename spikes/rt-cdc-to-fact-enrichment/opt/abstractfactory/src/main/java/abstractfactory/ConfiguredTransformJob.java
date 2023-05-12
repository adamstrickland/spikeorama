package abstractfactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.protobuf.util.JsonFormat;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

@Slf4j
public class ConfiguredTransformJob implements TransformJob {
  static String CONFIG_FILE = "facts.yml";

  Configuration configuration;

  public ConfiguredTransformJob() {
    this(CONFIG_FILE);
  }

  @SneakyThrows
  public ConfiguredTransformJob(String configFile) {
    var mapper = new ObjectMapper(new YAMLFactory());
    mapper.findAndRegisterModules();

    var cl = this.getClass().getClassLoader();
    this.configuration = mapper.readValue(cl.getResourceAsStream(configFile), Configuration.class);
  }

  @Override
  @SneakyThrows
  public void configure(StreamExecutionEnvironment env, String bootstrapServers) {
    this.configuration.facts.forEach(fsconfig -> {
      KafkaSource<String> source = KafkaSource.<String>builder()
          .setBootstrapServers(bootstrapServers)
          .setTopics(fsconfig.in.topic)
          .setGroupId("factfactory")
          .setStartingOffsets(OffsetsInitializer.earliest())
          .setValueOnlyDeserializer(new SimpleStringSchema())
          .build();

      DataStreamSource<String> streamSource = env.fromSource(
          source,
          WatermarkStrategy.noWatermarks(),
          "Kafka Source");

      KafkaSink<String> sink = KafkaSink.<String>builder()
          .setBootstrapServers(bootstrapServers)
          .setRecordSerializer(KafkaRecordSerializationSchema.builder()
              .setTopic(fsconfig.out.topic)
              .setValueSerializationSchema(new SimpleStringSchema())
              .build())
          .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
          .build();

      DataStream<String> outStream = streamSource.map(new MapFunction<String, String>() {
        @Override
        public String map(String in) throws Exception {
          // log.info("RECV: "+in);

          DocumentContext ctx = JsonPath.parse(in);

          PrimaryOuterClass.Primary.Builder builder = PrimaryOuterClass.Primary.newBuilder();

          fsconfig.out.attributes.forEach(aconfig -> {
            try {
              Class<? extends AttributeTransformation<String>> transformationClass = aconfig.getTransformer();
              Constructor<?> c = transformationClass.getConstructor();
              AttributeTransformation<Serializable> transformation = (AttributeTransformation<Serializable>) c
                  .newInstance();

              Class<? extends Serializable> extractedType = aconfig.getType();
              Object extractedValue = ctx.read(aconfig.expr, extractedType);
              Serializable translatedValue = transformation.transform(extractedValue);

              String setterName = "set" + aconfig.name.substring(0, 1).toUpperCase() + aconfig.name.substring(1);
              Method setter = builder.getClass().getDeclaredMethod(setterName, String.class);

              setter.invoke(builder, translatedValue.toString());
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });

          PrimaryOuterClass.Primary message = builder.build();
          JsonFormat.Printer printer = JsonFormat.printer();
          String out = printer.print(message);

          // log.info("SEND: "+out);
          return out;
        }
      });

      outStream.sinkTo(sink);
    });
  }
}
