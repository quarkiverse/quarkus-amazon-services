package io.quarkiverse.amazonservices.aws.crt.deployment;

import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ExcludeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import software.amazon.awssdk.crt.CRT;

public class AwsCrtProcessor {
    private static final String CRT_LIB_NAME = "aws-crt-jni";

    static final String CRT_JAR_MATCH_REGEX = "aws-crt";
    static final String NATIVE_IMAGE_RESOURCE_CONFIG_MATCH_REGEX = "/META-INF/native-image/software\\.amazon\\.awssdk/crt/aws-crt/resource-config\\.json";

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

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    public void resources(
            BuildProducer<NativeImageResourceBuildItem> resource,
            NativeImageRunnerBuildItem nativeImageRunnerFactory,
            BuildProducer<ExcludeConfigBuildItem> nativeImageExclusions) {

        // add linux x64 native lib when targeting containers
        if (nativeImageRunnerFactory.isContainerBuild()) {
            String libraryName = "libaws-crt-jni.so";
            String dir = "linux/x86_64";
            String glibResourcePath = dir + "/glibc/" + libraryName;
            resource.produce(new NativeImageResourceBuildItem(glibResourcePath));
        }
        // otherwise the native lib of the platform this build runs on
        else {
            String libraryName = System.mapLibraryName(CRT_LIB_NAME);
            String os = CRT.getOSIdentifier();
            String dir = os + "/" + CRT.getArchIdentifier() + "/" + CRT.getCRuntime(os);
            String libResourcePath = dir + "/" + libraryName;
            resource.produce(new NativeImageResourceBuildItem(libResourcePath));
        }

        // Excludes resource-config.json, which is reimplemented above to reduce total size.
        nativeImageExclusions.produce(new ExcludeConfigBuildItem(CRT_LIB_NAME, NATIVE_IMAGE_RESOURCE_CONFIG_MATCH_REGEX));
    }
}
