package io.quarkiverse.amazonservices.aws.crt.deployment;

import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

public class AwsCrtProcessor {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        // CRT and all types that statically init CRT
        // https://github.com/search?q=repo%3Aawslabs%2Faws-crt-java+static+%7B+new+CRT%28%29%3B+%7D&type=code
        Stream.of(
                "software.amazon.awssdk.crt.CRT",
                "software.amazon.awssdk.crt.CrtRuntimeException",
                "software.amazon.awssdk.crt.CrtResource",
                "software.amazon.awssdk.crt.Log",
                "software.amazon.awssdk.crt.io.Uri",
                "software.amazon.awssdk.crt.checksums.CRC32",
                "software.amazon.awssdk.crt.checksums.CRC32C",
                "software.amazon.awssdk.crt.checksums.CRC64NVME",
                "software.amazon.awssdk.crt.utils.StringUtils")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);
    }
}
