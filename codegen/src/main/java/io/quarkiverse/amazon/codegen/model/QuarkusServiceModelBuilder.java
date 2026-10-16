package io.quarkiverse.amazon.codegen.model;

import io.quarkiverse.amazon.codegen.QuarkusCustomizationConfig;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

/**
 * Builds a {@link QuarkusServiceModel} from the service models, the Quarkus customization and the module sources.
 */
public class QuarkusServiceModelBuilder {
    private final C2jModels models;
    private final IntermediateModel intermediateModel;
    private QuarkusCustomizationConfig quarkusCustomizationConfig = QuarkusCustomizationConfig.create();
    private String sourcesDirectory;

    public QuarkusServiceModelBuilder(C2jModels models) {
        this.models = models;
        this.intermediateModel = null;
    }

    public QuarkusServiceModelBuilder(IntermediateModel intermediateModel) {
        this.models = null;
        this.intermediateModel = intermediateModel;
    }

    public QuarkusServiceModelBuilder quarkusCustomizationConfig(QuarkusCustomizationConfig quarkusCustomizationConfig) {
        if (quarkusCustomizationConfig != null) {
            this.quarkusCustomizationConfig = quarkusCustomizationConfig;
        }
        return this;
    }

    public QuarkusServiceModelBuilder sourcesDirectory(String sourcesDirectory) {
        this.sourcesDirectory = sourcesDirectory;
        return this;
    }

    public QuarkusServiceModel build() {
        IntermediateModel model = intermediateModel != null ? intermediateModel
                : new IntermediateModelBuilder(models).build();
        String clientPackageName = model.getMetadata().getClientPackageName();
        QuarkusCustomizationConfig config = quarkusCustomizationConfig;

        return new QuarkusServiceModel(model,
                "io.quarkiverse.amazon." + (config.getPackageName() != null ? config.getPackageName() : clientPackageName),
                config.getClassPrefix() != null ? config.getClassPrefix() : model.getMetadata().getServiceName(),
                config.getConfigName() != null ? config.getConfigName() : clientPackageName,
                config.getFeatureName() != null ? config.getFeatureName() : "amazon-sdk-" + clientPackageName,
                sourcesDirectory);
    }
}
