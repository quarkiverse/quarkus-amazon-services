/*
 * Part of this file is derived from the AWS SDK for Java 2.x, which is licensed under the Apache License, Version 2.0.
 * https://github.com/aws/aws-sdk-java-v2/blob/40b8869c5a24ceae5c41e520ab43e7fbfd06187a/codegen/src/test/java/software/amazon/awssdk/codegen/poet/ClientTestModels.java
 */
package io.quarkiverse.amazon.codegen.poet;

import java.io.File;

import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class ClientTestModels {

    public static IntermediateModel restJsonServiceModels() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/rest-json/service-2.json").getFile());
        File customizationModel = new File(
                ClientTestModels.class.getResource("client/c2j/rest-json/customization.config").getFile());
        C2jModels models = C2jModels.builder()
                .serviceModel(getServiceModel(serviceModel))
                .customizationConfig(getCustomizationConfig(customizationModel))
                .build();

        return new IntermediateModelBuilder(models).build();
    }

    private static ServiceModel getServiceModel(File file) {
        return ModelLoaderUtils.loadModel(ServiceModel.class, file);
    }

    private static CustomizationConfig getCustomizationConfig(File file) {
        return ModelLoaderUtils.loadModel(CustomizationConfig.class, file);
    }
}
