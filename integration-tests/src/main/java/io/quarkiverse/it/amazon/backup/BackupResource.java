package io.quarkiverse.it.amazon.backup;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkiverse.amazon.common.AmazonClient;
import software.amazon.awssdk.services.backup.BackupAsyncClient;
import software.amazon.awssdk.services.backup.BackupClient;

@Path("/backup")
public class BackupResource {
    @Inject
    BackupClient sync;

    @Inject
    BackupAsyncClient async;

    @Inject
    @AmazonClient("archive")
    BackupClient namedSync;

    @Inject
    @AmazonClient("archive")
    BackupAsyncClient namedAsync;

    @POST
    @Path("/sync/{name}")
    public String create(@PathParam("name") String name) {
        return sync.createBackupVault(request -> request.backupVaultName(name)).backupVaultName();
    }

    @GET
    @Path("/sync/{name}")
    public String read(@PathParam("name") String name) {
        return namedSync.describeBackupVault(request -> request.backupVaultName(name)).backupVaultName();
    }

    @POST
    @Path("/async/{name}")
    public CompletionStage<String> createAsync(@PathParam("name") String name) {
        return async.createBackupVault(request -> request.backupVaultName(name))
                .thenApply(response -> response.backupVaultName());
    }

    @GET
    @Path("/async/{name}")
    public CompletionStage<String> readAsync(@PathParam("name") String name) {
        return namedAsync.describeBackupVault(request -> request.backupVaultName(name))
                .thenApply(response -> response.backupVaultName());
    }

    @DELETE
    @Path("/{name}")
    public void delete(@PathParam("name") String name) {
        sync.deleteBackupVault(request -> request.backupVaultName(name));
    }

}
