package io.quarkiverse.it.amazon.appconfigdata;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.awssdk.services.appconfig.model.Application;
import software.amazon.awssdk.services.appconfig.model.ConfigurationProfileSummary;
import software.amazon.awssdk.services.appconfig.model.CreateHostedConfigurationVersionResponse;
import software.amazon.awssdk.services.appconfig.model.DeploymentStrategy;
import software.amazon.awssdk.services.appconfig.model.Environment;

@Path("/appconfigdata")
public class AppConfigDataResource {

    @Inject
    AppConfigClient appConfigClient;

    @ConfigProperty(name = "application", defaultValue = "N/A")
    String applicationName;
    @ConfigProperty(name = "host", defaultValue = "N/A")
    Provider<String> host;

    @ConfigProperty(name = "port", defaultValue = "N/A")
    Provider<String> port;

    @GET
    @Path("app-name")
    @Produces(TEXT_PLAIN)
    public String testApplicationName() {
        return "application: " + applicationName;
    }

    @GET
    @Path("db-config")
    @Produces(TEXT_PLAIN)
    public String testDbConfig() {
        return "host: " + host.get() + ", port: " + port.get();
    }

    @PUT
    @Path("config")
    @Consumes(TEXT_PLAIN)
    public void updateTestConfig(String newConfig) {
        Application application = appConfigClient.listApplications(b -> b.maxResults(10))
                .items()
                .getFirst();

        ConfigurationProfileSummary configurationProfile = appConfigClient
                .listConfigurationProfiles(b -> b.applicationId(application.id()))
                .items()
                .getFirst();

        Environment environment = appConfigClient.listEnvironments(b -> b.applicationId(application.id()))
                .items()
                .getFirst();

        CreateHostedConfigurationVersionResponse response = appConfigClient
                .createHostedConfigurationVersion(b -> b.applicationId(application.id())
                        .configurationProfileId(configurationProfile.id())
                        .content(SdkBytes.fromUtf8String(newConfig))
                        .contentType("application/json"));

        DeploymentStrategy deploymentStrategy = appConfigClient.listDeploymentStrategies(b -> b.maxResults(10))
                .items()
                .getFirst();

        appConfigClient.startDeployment(b -> b.applicationId(application.id())
                .configurationProfileId(configurationProfile.id())
                .configurationVersion(response.versionNumber().toString())
                .deploymentStrategyId(deploymentStrategy.id())
                .environmentId(environment.id()));
    }
}
