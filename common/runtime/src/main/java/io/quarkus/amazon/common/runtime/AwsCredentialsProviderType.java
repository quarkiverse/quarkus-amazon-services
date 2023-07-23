package io.quarkus.amazon.common.runtime;

import io.quarkus.amazon.common.runtime.AwsCredentialsProviderConfig.ProfileCredentialsProviderConfig;
import io.quarkus.arc.Arc;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProcessCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;

public enum AwsCredentialsProviderType {
    DEFAULT {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            return DefaultCredentialsProvider.builder()
                    .asyncCredentialUpdateEnabled(config.defaultProvider().asyncCredentialUpdateEnabled())
                    .reuseLastProviderEnabled(config.defaultProvider().reuseLastProviderEnabled()).build();
        }
    },
    STATIC {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            if (!config.staticProvider().accessKeyId().isPresent()
                    || !config.staticProvider().secretAccessKey().isPresent()) {
                throw new RuntimeConfigurationError(
                        String.format("%1$s.aws.credentials.static-provider.access-key-id and "
                                + "%1$s.aws.credentials.static-provider.secret-access-key cannot be empty if STATIC credentials provider used.",
                                configKeyRoot));
            }
            var accessKeyId = config.staticProvider().accessKeyId().get();
            var secretAccessKey = config.staticProvider().secretAccessKey().get();
            return StaticCredentialsProvider.create(
                    config.staticProvider().sessionToken()
                            .map(sessionToken -> (AwsCredentials) AwsSessionCredentials
                                    .create(accessKeyId, secretAccessKey, sessionToken))
                            .orElseGet(() -> AwsBasicCredentials
                                    .create(accessKeyId, secretAccessKey)));
        }
    },

    SYSTEM_PROPERTY {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            return SystemPropertyCredentialsProvider.create();
        }
    },
    ENV_VARIABLE {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            return EnvironmentVariableCredentialsProvider.create();
        }
    },
    PROFILE {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            ProfileCredentialsProviderConfig cfg = config.profileProvider();
            ProfileCredentialsProvider.Builder builder = ProfileCredentialsProvider.builder();
            cfg.profileName().ifPresent(builder::profileName);

            return builder.build();
        }
    },
    CONTAINER {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            return ContainerCredentialsProvider.builder().build();
        }
    },
    INSTANCE_PROFILE {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            return InstanceProfileCredentialsProvider.builder().build();
        }
    },
    PROCESS {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            if (!config.processProvider().command().isPresent()) {
                throw new RuntimeConfigurationError(
                        String.format(
                                "%s.aws.credentials.process-provider.command cannot be empty if PROCESS credentials provider used.",
                                configKeyRoot));
            }
            ProcessCredentialsProvider.Builder builder = ProcessCredentialsProvider.builder()
                    .asyncCredentialUpdateEnabled(config.processProvider().asyncCredentialUpdateEnabled());

            builder.credentialRefreshThreshold(config.processProvider().credentialRefreshThreshold());
            builder.processOutputLimit(config.processProvider().processOutputLimit().asLongValue());
            builder.command(config.processProvider().command().get());

            return builder.build();
        }
    },
    CUSTOM {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            String beanName = config.customProvider().name().orElseThrow(() -> new RuntimeConfigurationError(
                    String.format(
                            "%s.aws.credentials.custom-provider.name cannot be empty if CUSTOM credentials provider used.",
                            configKeyRoot)));
            AwsCredentialsProvider credentialsProvider = (AwsCredentialsProvider) Arc.container().instance(beanName).get();
            if (credentialsProvider == null) {
                throw new RuntimeConfigurationError(
                        String.format("cannot find bean '%s' specified in %s.aws.credentials.custom-provider.name", beanName,
                                configKeyRoot));
            }
            return credentialsProvider;
        }
    },
    ANONYMOUS {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            return AnonymousCredentialsProvider.create();
        }
    };

    @Deprecated
    public final AwsCredentialsProvider create(AwsCredentialsProviderConfig config) {
        return create(config, "");
    }

    public abstract AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot);
}
