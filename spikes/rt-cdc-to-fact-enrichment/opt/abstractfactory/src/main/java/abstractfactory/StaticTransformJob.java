package abstractfactory;

import com.google.protobuf.util.JsonFormat;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
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

@Slf4j
public class StaticTransformJob implements TransformJob {
  @Override
  public void configure(StreamExecutionEnvironment env, String bootstrapServers) {
    KafkaSource<String> source = KafkaSource.<String>builder()
        .setBootstrapServers(bootstrapServers)
        .setTopics("monolith.public.wildapp_primary")
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
            .setTopic("wildapp.primary.facts")
            .setValueSerializationSchema(new SimpleStringSchema())
            .build())
        .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
        .build();

    DataStream<String> outStream = streamSource.map(new  MapFunction<String, String>() {
      @Override
      public String map(String in) throws Exception {
        log.info("RECV: "+in);

        DocumentContext ctx = JsonPath.parse(in);

        String canonicalId = ctx.read("$.after.canonical_id");
        String name = ctx.read("$.after.name");
        Long ts_ms = ctx.read("$.ts_ms");
        String ts = new LongToIso8601AttributeTransformation().transform(ts_ms);
        PrimaryOuterClass.Primary primary = PrimaryOuterClass.Primary.newBuilder()
            .setCanonicalId(canonicalId)
            .setName(name)
            .setUpdatedAt(ts)
            .build();

        JsonFormat.Printer printer = JsonFormat.printer();
        String out = printer.print(primary);

        log.info("SEND: "+out);
        return out;
      }
    });

    outStream.sinkTo(sink);
  }
}
