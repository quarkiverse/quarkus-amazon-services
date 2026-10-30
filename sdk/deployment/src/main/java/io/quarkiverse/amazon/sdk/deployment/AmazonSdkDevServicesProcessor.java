package io.quarkiverse.amazon.sdk.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkiverse.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackExtensionProvider;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig.AwsStack;
import io.quarkiverse.amazon.sdk.runtime.AmazonSdkBuildTimeConfig;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.smallrye.config.DefaultValuesConfigSource;

@BuildSteps(onlyIf = IsDevServicesSupportedByLaunchMode.class)
public class AmazonSdkDevServicesProcessor {
    @BuildStep
    void setup(List<AmazonSdkServiceBuildItem> services, List<RequireAmazonClientInjectionBuildItem> injections,
            AmazonSdkBuildTimeConfig config, GlobalDevServicesBuildTimeConfig globalConfig,
            BuildProducer<DevServicesAwsStackProviderBuildItem> providers) {
        if (!config.devservices().enabled().orElse(true)) {
            return;
        }
        for (AmazonSdkServiceBuildItem service : services) {
            List<String> clients = injections.stream()
                    .filter(i -> i.getClassName().equals(service.syncClient) || i.getClassName().equals(service.asyncClient))
                    .map(RequireAmazonClientInjectionBuildItem::getName).distinct().sorted().toList();
            providers(service.service, clients, config, globalConfig, ConfigProvider.getConfig()).forEach(providers::produce);
        }
    }

    static List<DevServicesAwsStackProviderBuildItem> providers(String service, List<String> clients,
            AmazonSdkBuildTimeConfig config, GlobalDevServicesBuildTimeConfig globalConfig, Config properties) {
        AwsStack defaultProvider = config.devservices().provider().orElse(globalConfig.provider());
        Map<AwsStack, List<String>> groups = clients.stream().collect(Collectors.groupingBy(name -> {
            var named = config.clients().get(name);
            return named == null ? defaultProvider : named.devservices().provider().orElse(defaultProvider);
        }));
        List<DevServicesAwsStackProviderBuildItem> result = new ArrayList<>();
        groups.forEach((stack, names) -> {
            var settings = new StackSettings(config.devservices().enabled(), config.devservices().containerProperties(),
                    Optional.of(stack));
            var request = provider(service, names, settings, properties);
            if (request != null) {
                result.add(request);
            }
        });
        return result;
    }

    private record StackSettings(Optional<Boolean> enabled, Map<String, String> containerProperties,
            Optional<AwsStack> provider) implements DevServicesBuildTimeConfig {
    }

    static DevServicesAwsStackProviderBuildItem provider(String service, List<String> clients,
            DevServicesBuildTimeConfig settings, Config config) {
        List<String> localClients = clients.stream()
                .filter(name -> prefixes(name).stream()
                        .noneMatch(p -> config.getOptionalValue(p + ".endpoint-override", String.class).isPresent()))
                .toList();
        if (localClients.isEmpty() || !settings.enabled().orElse(true)) {
            return null;
        }
        return new DevServicesAwsStackProviderBuildItem(service, settings, new DevServicesAwsStackExtensionProvider() {
            @Override
            public void prepareAwsStackContainer(AwsStackContainer stack) {
            }

            @Override
            public void reuseAwsStackContainer(AwsStackContainer stack) {
            }

            @Override
            public Function<AwsStackContainer, Map<String, String>> getClientConfig() {
                return stack -> {
                    Map<String, String> defaults = new HashMap<>();
                    for (String name : localClients) {
                        // Keep default-client values separate from shared defaults inherited by external clients.
                        String target = clientPrefix(config, name);
                        List<String> layers = prefixes(name);
                        defaults.put(target + ".endpoint-override", stack.getEndpoint().toString());
                        if (layers.stream().noneMatch(p -> config.getOptionalValue(p + ".region", String.class).isPresent())) {
                            defaults.put(target + ".region", stack.getRegion());
                        }
                        if (layers.stream().noneMatch(p -> hasCredentials(config, p))) {
                            defaults.put(target + ".credentials.type", "static");
                            defaults.put(target + ".credentials.static-provider.access-key-id", stack.getAccessKey());
                            defaults.put(target + ".credentials.static-provider.secret-access-key", stack.getSecretKey());
                        }
                    }
                    return defaults;
                };
            }
        });
    }

    private static boolean hasCredentials(Config config, String prefix) {
        if (configured(config, prefix + ".credentials.type")) {
            return true;
        }
        for (String property : config.getPropertyNames()) {
            String name = property.startsWith("%") ? property.substring(property.indexOf('.') + 1) : property;
            if (name.startsWith(prefix + ".credentials.") && configured(config, name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean configured(Config config, String name) {
        // Mapping defaults (e.g. credentials.type=default) do not constitute a user identity.
        return config.getConfigValue(name).getSourceOrdinal() > DefaultValuesConfigSource.ORDINAL
                && config.getOptionalValue(name, String.class).isPresent();
    }

    private static String clientPrefix(Config config, String name) {
        // Use the same quoted/unquoted map entry as the user's configuration.
        for (String prefix : prefixes(name)) {
            if (prefix.equals("quarkus.aws")) {
                continue;
            }
            for (String property : config.getPropertyNames()) {
                String actual = property.startsWith("%") ? property.substring(property.indexOf('.') + 1) : property;
                if (actual.startsWith(prefix + ".") && configured(config, actual)) {
                    return prefix;
                }
            }
        }
        return "quarkus.aws." + key(name);
    }

    private static List<String> prefixes(String name) {
        List<String> prefixes = new ArrayList<>();
        if (!ClientUtil.isDefaultClient(name)) {
            prefixes.add("quarkus.aws." + key(name));
            prefixes.add("quarkus.aws.\"" + name + '"');
        }
        prefixes.add("quarkus.aws");
        return prefixes;
    }

    private static String key(String name) {
        return name.indexOf('.') >= 0 || ClientUtil.isDefaultClient(name) ? '"' + name + '"' : name;
    }
}
