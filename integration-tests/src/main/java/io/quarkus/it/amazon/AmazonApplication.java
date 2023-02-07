package io.quarkus.it.amazon;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/test")
public class AmazonApplication extends Application {
}