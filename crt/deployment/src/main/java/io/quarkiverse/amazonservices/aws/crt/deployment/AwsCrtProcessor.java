package io.quarkiverse.amazonservices.aws.crt.deployment;

import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import software.amazon.awssdk.crt.CRT;

public class AwsCrtProcessor {
    private static final String CRT_LIB_NAME = "aws-crt-jni";

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        Stream.of(
                "software.amazon.awssdk.crt.CRT",
                "software.amazon.awssdk.crt.CrtRuntimeException",
                "software.amazon.awssdk.crt.CrtResource",
                "software.amazon.awssdk.crt.Log")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    public void resources(
            BuildProducer<NativeImageResourceBuildItem> resource,
            NativeImageRunnerBuildItem nativeImageRunnerFactory) {

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
    }

    /*
     * register jni reflect config until embedded in aws crt. see https://github.com/aws/aws-sdk-java-v2/issues/2948
     * at startup, aws-crt cache all java class ids :
     * https://github.com/awslabs/aws-crt-java/blob/main/src/native/java_class_ids.c
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    public void registerAwsCrtJniRuntimeAccessBuildItem(
            BuildProducer<JniRuntimeAccessBuildItem> jniRuntimeAccess,
            BuildProducer<JniRuntimeAccessMethodBuildItem> jniRuntimeAccessMethod,
            BuildProducer<JniRuntimeAccessFieldBuildItem> jniRuntimeAccessField) {
        jniRuntimeAccessMethod
                .produce(new JniRuntimeAccessMethodBuildItem("java.lang.Boolean", "<init>", "boolean"));
        jniRuntimeAccessMethod
                .produce(new JniRuntimeAccessMethodBuildItem("java.lang.Boolean", "booleanValue"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem("java.lang.Boolean", "getBoolean",
                "java.lang.String"));
        jniRuntimeAccessMethod
                .produce(new JniRuntimeAccessMethodBuildItem("java.lang.Integer", "<init>", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem("java.lang.Integer", "intValue"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem("java.lang.Long", "<init>", "long"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem("java.lang.Long", "longValue"));
        jniRuntimeAccessMethod
                .produce(new JniRuntimeAccessMethodBuildItem("java.lang.String", "lastIndexOf", "int"));
        jniRuntimeAccessMethod
                .produce(new JniRuntimeAccessMethodBuildItem("java.lang.String", "substring", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem("java.lang.System", "getProperty",
                "java.lang.String"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem("java.lang.System", "setProperty",
                "java.lang.String", "java.lang.String"));
        jniRuntimeAccess.produce(new JniRuntimeAccessBuildItem(true, true, false, "java.nio.Buffer"));
        jniRuntimeAccess.produce(new JniRuntimeAccessBuildItem(true, true, false, "java.nio.ByteBuffer"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem("java.util.ArrayList", "<init>"));
        jniRuntimeAccessMethod.produce(
                new JniRuntimeAccessMethodBuildItem("java.util.List", "add", "java.lang.Object"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem("java.util.List", "get", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem("java.util.List", "size"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "java.util.concurrent.CompletableFuture", "complete", "java.lang.Object"));
        jniRuntimeAccessMethod
                .produce(new JniRuntimeAccessMethodBuildItem("java.util.concurrent.CompletableFuture",
                        "completeExceptionally", "java.lang.Throwable"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem("java.util.function.Predicate",
                "test", "java.lang.Object"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.AsyncCallback", "onFailure", "java.lang.Throwable"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.AsyncCallback", "onSuccess"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.AsyncCallback", "onSuccess", "java.lang.Object"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem("software.amazon.awssdk.crt.CRT",
                "testJniException", "boolean"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.CrtResource", "addRef"));
        jniRuntimeAccessMethod.produce(
                new JniRuntimeAccessMethodBuildItem("software.amazon.awssdk.crt.CrtResource", "close"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.CrtResource", "getNativeHandle"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.CrtResource", "releaseReferences"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.CrtRuntimeException", "<init>", "int"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.CrtRuntimeException", "errorCode"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.SystemInfo$CpuInfo", "<init>", "int", "boolean"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.auth.credentials.Credentials", "<init>"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.credentials.Credentials", "accessKeyId"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.credentials.Credentials", "secretAccessKey"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.credentials.Credentials", "sessionToken"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.auth.credentials.CredentialsProvider",
                "onGetCredentialsComplete", "java.util.concurrent.CompletableFuture",
                "software.amazon.awssdk.crt.auth.credentials.Credentials"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.auth.credentials.CredentialsProvider",
                "onShutdownComplete"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.auth.credentials.DelegateCredentialsHandler",
                "getCredentials"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.signing.AwsSigningConfig", "algorithm"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.signing.AwsSigningConfig", "credentials"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.signing.AwsSigningConfig", "credentialsProvider"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.signing.AwsSigningConfig", "expirationInSeconds"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.signing.AwsSigningConfig", "omitSessionToken"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.signing.AwsSigningConfig", "region"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.signing.AwsSigningConfig", "service"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.signing.AwsSigningConfig", "shouldNormalizeUriPath"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.signing.AwsSigningConfig", "shouldSignHeader"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.signing.AwsSigningConfig", "signatureType"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.signing.AwsSigningConfig", "signedBodyHeader"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.signing.AwsSigningConfig", "signedBodyValue"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.signing.AwsSigningConfig", "time"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.signing.AwsSigningConfig", "useDoubleUriEncode"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.auth.signing.AwsSigningResult", "<init>"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.signing.AwsSigningResult", "signature"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.auth.signing.AwsSigningResult", "signedRequest"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.cal.EccKeyPair", "<init>", "long"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.eventstream.ClientConnectionContinuationHandler",
                "onContinuationClosedShim"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.eventstream.ClientConnectionContinuationHandler",
                "onContinuationMessageShim", "byte[]", "byte[]", "int", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.eventstream.ClientConnectionHandler",
                "onConnectionClosedShim", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.eventstream.ClientConnectionHandler",
                "onConnectionSetupShim", "long", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.eventstream.ClientConnectionHandler", "onProtocolMessage",
                "byte[]", "byte[]", "int", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.eventstream.MessageFlushCallback", "onCallbackInvoked",
                "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.eventstream.ServerConnection", "<init>", "long"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.eventstream.ServerConnectionContinuation", "<init>",
                "long"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.eventstream.ServerConnectionContinuationHandler",
                "onContinuationClosedShim"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.eventstream.ServerConnectionContinuationHandler",
                "onContinuationMessageShim", "byte[]", "byte[]", "int", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.eventstream.ServerConnectionHandler", "onIncomingStream",
                "software.amazon.awssdk.crt.eventstream.ServerConnectionContinuation", "byte[]"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.eventstream.ServerConnectionHandler", "onProtocolMessage",
                "byte[]", "byte[]", "int", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.eventstream.ServerListener", "onShutdownComplete"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.eventstream.ServerListenerHandler",
                "onConnectionShutdownShim", "software.amazon.awssdk.crt.eventstream.ServerConnection",
                "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.eventstream.ServerListenerHandler", "onNewConnection",
                "software.amazon.awssdk.crt.eventstream.ServerConnection", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.Http2Stream", "<init>", "long"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.Http2StreamManager", "onShutdownComplete"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpClientConnection", "onConnectionAcquired",
                "java.util.concurrent.CompletableFuture", "long", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpClientConnectionManager", "onShutdownComplete"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpHeader", "<init>", "byte[]", "byte[]"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpManagerMetrics", "<init>", "long", "long",
                "long"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpProxyOptions", "getAuthorizationPassword"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpProxyOptions", "getAuthorizationType"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpProxyOptions", "getAuthorizationUsername"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpProxyOptions", "getConnectionType"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpProxyOptions", "getHost"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpProxyOptions", "getPort"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpProxyOptions", "getTlsContext"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpProxyOptions$HttpProxyConnectionType",
                "getValue"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpRequest", "<init>", "java.nio.ByteBuffer",
                "software.amazon.awssdk.crt.http.HttpRequestBodyStream"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.http.HttpRequestBase", "bodyStream"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpRequestBodyStream", "getLength"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpRequestBodyStream", "resetPosition"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpRequestBodyStream", "sendRequestBody",
                "java.nio.ByteBuffer"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpStream", "<init>", "long"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpStream$HttpStreamWriteChunkCompletionCallback",
                "onChunkCompleted", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpStreamResponseHandlerNativeAdapter",
                "onResponseBody", "software.amazon.awssdk.crt.http.HttpStreamBase",
                "java.nio.ByteBuffer"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpStreamResponseHandlerNativeAdapter",
                "onResponseComplete", "software.amazon.awssdk.crt.http.HttpStreamBase", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpStreamResponseHandlerNativeAdapter",
                "onResponseHeaders", "software.amazon.awssdk.crt.http.HttpStreamBase", "int", "int",
                "java.nio.ByteBuffer"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.http.HttpStreamResponseHandlerNativeAdapter",
                "onResponseHeadersDone", "software.amazon.awssdk.crt.http.HttpStreamBase", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.io.ClientBootstrap", "onShutdownComplete"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.io.DirectoryEntry", "<init>"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.DirectoryEntry", "fileSize"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.DirectoryEntry", "isDirectory"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.DirectoryEntry", "isFile"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.DirectoryEntry", "isSymLink"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.DirectoryEntry", "path"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.DirectoryEntry", "relativePath"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.io.DirectoryTraversalHandler", "onDirectoryEntry",
                "software.amazon.awssdk.crt.io.DirectoryEntry"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.io.EventLoopGroup", "onCleanupComplete"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.io.ExponentialBackoffRetryOptions", "<init>"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.ExponentialBackoffRetryOptions",
                "backoffScaleFactorMS"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.ExponentialBackoffRetryOptions", "eventLoopGroup"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.ExponentialBackoffRetryOptions", "jitterMode"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.ExponentialBackoffRetryOptions", "maxRetries"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.io.ExponentialBackoffRetryOptions$JitterMode", "getValue"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.ExponentialBackoffRetryOptions$JitterMode", "value"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.io.StandardRetryOptions", "<init>"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.StandardRetryOptions", "backoffRetryOptions"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.StandardRetryOptions", "initialBucketCapacity"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.TlsContextCustomKeyOperationOptions",
                "certificateFileContents"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.TlsContextCustomKeyOperationOptions",
                "certificateFilePath"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.TlsContextCustomKeyOperationOptions",
                "operationHandler"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.TlsContextPkcs11Options", "certificateFileContents"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.TlsContextPkcs11Options", "certificateFilePath"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.TlsContextPkcs11Options", "pkcs11Lib"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.TlsContextPkcs11Options", "privateKeyObjectLabel"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.TlsContextPkcs11Options", "slotId"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.TlsContextPkcs11Options", "tokenLabel"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.io.TlsContextPkcs11Options", "userPin"));
        jniRuntimeAccessMethod.produce(
                new JniRuntimeAccessMethodBuildItem("software.amazon.awssdk.crt.io.TlsKeyOperation",
                        "<init>", "long", "byte[]", "int", "int", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.io.TlsKeyOperation", "invokePerformOperation",
                "software.amazon.awssdk.crt.io.TlsKeyOperationHandler",
                "software.amazon.awssdk.crt.io.TlsKeyOperation"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.io.TlsKeyOperationHandler", "performOperation",
                "software.amazon.awssdk.crt.io.TlsKeyOperation"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt.MqttClientConnection", "onConnectionComplete", "int",
                "boolean"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt.MqttClientConnection", "onConnectionInterrupted",
                "int", "software.amazon.awssdk.crt.AsyncCallback"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt.MqttClientConnection", "onConnectionResumed",
                "boolean"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt.MqttClientConnection", "onConnectionClosed"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt.MqttClientConnection", "onWebsocketHandshake",
                "software.amazon.awssdk.crt.http.HttpRequest", "long"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt.MqttClientConnection", "onConnectionSuccess",
                "boolean"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt.MqttClientConnection", "onConnectionFailure",
                "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt.MqttClientConnection$MessageHandler", "deliver",
                "java.lang.String", "byte[]", "boolean", "int", "boolean"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt.MqttClientConnectionOperationStatistics", "<init>"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt.MqttClientConnectionOperationStatistics",
                "incompleteOperationCount"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt.MqttClientConnectionOperationStatistics",
                "incompleteOperationSize"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt.MqttClientConnectionOperationStatistics",
                "unackedOperationCount"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt.MqttClientConnectionOperationStatistics",
                "unackedOperationSize"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt.MqttException", "<init>", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5Client", "onWebsocketHandshake",
                "software.amazon.awssdk.crt.http.HttpRequest", "long"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5Client", "setIsConnected", "boolean"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5Client", "websocketHandshakeTransform"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOperationStatistics", "<init>"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOperationStatistics",
                "incompleteOperationCount"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOperationStatistics",
                "incompleteOperationSize"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOperationStatistics",
                "unackedOperationCount"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOperationStatistics",
                "unackedOperationSize"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "getBootstrap"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions",
                "getExtendedValidationAndFlowControlOptions"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "getOfflineQueueBehavior"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "getRetryJitterMode"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "getSessionBehavior"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "getSocketOptions"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "getTlsContext"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "ackTimeoutSeconds"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "connackTimeoutMs"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions",
                "extendedValidationAndFlowControlOptions"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "hostName"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "httpProxyOptions"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "lifecycleEvents"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "maxReconnectDelayMs"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions",
                "minConnectedTimeToResetReconnectDelayMs"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "minReconnectDelayMs"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "offlineQueueBehavior"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "pingTimeoutMs"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "port"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "publishEvents"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "retryJitterMode"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions", "sessionBehavior"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions$ClientOfflineQueueBehavior",
                "getValue"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions$ClientSessionBehavior",
                "getValue"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions$ExtendedValidationAndFlowControlOptions",
                "getValue"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions$LifecycleEvents",
                "onAttemptingConnect", "software.amazon.awssdk.crt.mqtt5.Mqtt5Client",
                "software.amazon.awssdk.crt.mqtt5.OnAttemptingConnectReturn"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions$LifecycleEvents",
                "onConnectionFailure", "software.amazon.awssdk.crt.mqtt5.Mqtt5Client",
                "software.amazon.awssdk.crt.mqtt5.OnConnectionFailureReturn"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions$LifecycleEvents",
                "onConnectionSuccess", "software.amazon.awssdk.crt.mqtt5.Mqtt5Client",
                "software.amazon.awssdk.crt.mqtt5.OnConnectionSuccessReturn"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions$LifecycleEvents",
                "onDisconnection", "software.amazon.awssdk.crt.mqtt5.Mqtt5Client",
                "software.amazon.awssdk.crt.mqtt5.OnDisconnectionReturn"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions$LifecycleEvents", "onStopped",
                "software.amazon.awssdk.crt.mqtt5.Mqtt5Client",
                "software.amazon.awssdk.crt.mqtt5.OnStoppedReturn"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions$PublishEvents",
                "onMessageReceived", "software.amazon.awssdk.crt.mqtt5.Mqtt5Client",
                "software.amazon.awssdk.crt.mqtt5.PublishReturn"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.NegotiatedSettings", "<init>"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.NegotiatedSettings", "nativeSetQOS", "int"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.NegotiatedSettings", "assignedClientID"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.NegotiatedSettings", "maximumPacketSizeToServer"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.NegotiatedSettings", "maximumQOS"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.NegotiatedSettings", "receiveMaximumFromServer"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.NegotiatedSettings", "rejoinedSession"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.NegotiatedSettings", "retainAvailable"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.NegotiatedSettings", "serverKeepAlive"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.NegotiatedSettings", "sessionExpiryInterval"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.NegotiatedSettings", "sharedSubscriptionsAvailable"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.NegotiatedSettings",
                "subscriptionIdentifiersAvailable"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.NegotiatedSettings",
                "wildcardSubscriptionsAvailable"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.OnAttemptingConnectReturn", "<init>"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.OnConnectionFailureReturn", "<init>", "int",
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.OnConnectionSuccessReturn", "<init>",
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket",
                "software.amazon.awssdk.crt.mqtt5.NegotiatedSettings"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.OnDisconnectionReturn", "<init>", "int",
                "software.amazon.awssdk.crt.mqtt5.packets.DisconnectPacket"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.OnStoppedReturn", "<init>"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.PublishResult", "<init>"));
        jniRuntimeAccessMethod.produce(
                new JniRuntimeAccessMethodBuildItem("software.amazon.awssdk.crt.mqtt5.PublishResult",
                        "<init>", "software.amazon.awssdk.crt.mqtt5.packets.PubAckPacket"));
        jniRuntimeAccessMethod.produce(
                new JniRuntimeAccessMethodBuildItem("software.amazon.awssdk.crt.mqtt5.PublishReturn",
                        "<init>", "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.QOS", "getEnumValueFromInteger", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.QOS", "getValue"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket", "<init>"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket", "nativeAddMaximumQOS",
                "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket", "nativeAddReasonCode",
                "int"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket", "assignedClientIdentifier"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket", "maximumPacketSize"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket", "maximumQOS"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket", "reasonCode"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket", "reasonString"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket", "receiveMaximum"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket", "responseInformation"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket", "retainAvailable"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket", "serverKeepAlive"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket", "serverReference"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket",
                "sessionExpiryIntervalSeconds"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket", "sessionPresent"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket",
                "sharedSubscriptionsAvailable"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket",
                "subscriptionIdentifiersAvailable"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket", "userProperties"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket",
                "wildcardSubscriptionsAvailable"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket$ConnectReasonCode",
                "getEnumValueFromInteger", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnAckPacket$ConnectReasonCode",
                "getValue"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnectPacket", "clientId"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnectPacket", "keepAliveIntervalSeconds"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnectPacket", "maximumPacketSizeBytes"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnectPacket", "password"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnectPacket", "receiveMaximum"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnectPacket", "requestProblemInformation"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnectPacket",
                "requestResponseInformation"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnectPacket",
                "sessionExpiryIntervalSeconds"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnectPacket", "userProperties"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnectPacket", "username"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnectPacket", "will"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.ConnectPacket", "willDelayIntervalSeconds"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.DisconnectPacket", "<init>"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.DisconnectPacket", "getReasonCode"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.DisconnectPacket",
                "nativeAddDisconnectReasonCode", "int"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.DisconnectPacket", "reasonCode"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.DisconnectPacket", "reasonString"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.DisconnectPacket", "serverReference"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.DisconnectPacket",
                "sessionExpiryIntervalSeconds"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.DisconnectPacket", "userProperties"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.DisconnectPacket$DisconnectReasonCode",
                "getEnumValueFromInteger", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.DisconnectPacket$DisconnectReasonCode",
                "getValue"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PubAckPacket", "<init>"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PubAckPacket", "nativeAddReasonCode", "int"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PubAckPacket", "reasonCode"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PubAckPacket", "reasonString"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PubAckPacket", "userProperties"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PubAckPacket$PubAckReasonCode",
                "getEnumValueFromInteger", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PubAckPacket$PubAckReasonCode", "getValue"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket", "<init>"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket", "getPayloadFormat"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket", "getQOS"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket",
                "nativeSetPayloadFormatIndicator", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket", "nativeSetQOS", "int"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket", "contentType"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket", "correlationData"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket",
                "messageExpiryIntervalSeconds"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket", "packetQOS"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket", "payload"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket", "payloadFormat"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket", "responseTopic"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket", "retain"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket", "subscriptionIdentifiers"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket", "topic"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket", "userProperties"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket$PayloadFormatIndicator",
                "getEnumValueFromInteger", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.PublishPacket$PayloadFormatIndicator",
                "getValue"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubAckPacket", "<init>"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubAckPacket", "nativeAddSubackCode", "int"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubAckPacket", "reasonCodes"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubAckPacket", "reasonString"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubAckPacket", "userProperties"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubAckPacket$SubAckReasonCode",
                "getEnumValueFromInteger", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubAckPacket$SubAckReasonCode", "getValue"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubscribePacket", "subscriptionIdentifier"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubscribePacket", "subscriptions"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubscribePacket", "userProperties"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubscribePacket$RetainHandlingType",
                "getValue"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubscribePacket$Subscription", "getNoLocal"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubscribePacket$Subscription", "getQOS"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubscribePacket$Subscription",
                "getRetainAsPublished"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubscribePacket$Subscription",
                "getRetainHandlingType"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubscribePacket$Subscription",
                "getTopicFilter"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubscribePacket$Subscription", "noLocal"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.SubscribePacket$Subscription",
                "retainAsPublished"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.UnsubAckPacket", "<init>"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.UnsubAckPacket", "nativeAddUnsubackCode",
                "int"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.UnsubAckPacket", "reasonCodes"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.UnsubAckPacket", "reasonString"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.UnsubAckPacket", "userProperties"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.UnsubAckPacket$UnsubAckReasonCode",
                "getEnumValueFromInteger", "int"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.UnsubAckPacket$UnsubAckReasonCode",
                "getValue"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.UnsubscribePacket", "subscriptions"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.UnsubscribePacket", "userProperties"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.UserProperty", "<init>", "java.lang.String",
                "java.lang.String"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.UserProperty", "key"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.mqtt5.packets.UserProperty", "value"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.s3.ResumeToken", "nativeType"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.s3.ResumeToken", "numPartsCompleted"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.s3.ResumeToken", "partSize"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.s3.ResumeToken", "totalNumParts"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.s3.ResumeToken", "uploadId"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.s3.S3Client", "onShutdownComplete"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.s3.S3MetaRequest", "onShutdownComplete"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.s3.S3MetaRequestProgress", "<init>"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.s3.S3MetaRequestProgress", "bytesTransferred"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.s3.S3MetaRequestProgress", "contentLength"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandlerNativeAdapter", "onFinished",
                "int", "int", "byte[]", "int", "boolean", "java.lang.Throwable"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandlerNativeAdapter", "onProgress",
                "software.amazon.awssdk.crt.s3.S3MetaRequestProgress"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandlerNativeAdapter",
                "onResponseBody", "byte[]", "long", "long"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandlerNativeAdapter",
                "onResponseHeaders", "int", "java.nio.ByteBuffer"));
        jniRuntimeAccessMethod.produce(new JniRuntimeAccessMethodBuildItem(
                "software.amazon.awssdk.crt.s3.S3TcpKeepAliveOptions", "<init>"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.s3.S3TcpKeepAliveOptions", "keepAliveIntervalSec"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.s3.S3TcpKeepAliveOptions", "keepAliveMaxFailedProbes"));
        jniRuntimeAccessField.produce(new JniRuntimeAccessFieldBuildItem(
                "software.amazon.awssdk.crt.s3.S3TcpKeepAliveOptions", "keepAliveTimeoutSec"));
    }
}
