package io.quarkiverse.amazon.common.deployment;

import java.util.function.BooleanSupplier;

public class AmazonHttpClients {

    public static final String APACHE_HTTP_SERVICE = "software.amazon.awssdk.http.apache.ApacheSdkHttpService";
    public static final String NETTY_HTTP_SERVICE = "software.amazon.awssdk.http.nio.netty.NettySdkAsyncHttpService";
    public static final String URL_CONNECTION_HTTP_SERVICE = "software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService";
    public static final String AWS_CRT_HTTP_SERVICE = "software.amazon.awssdk.http.crt.AwsCrtSdkHttpService";

    public static class IsAmazonApacheHttpServicePresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName(APACHE_HTTP_SERVICE);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    };

    public static class IsAmazonNettyHttpServicePresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName(NETTY_HTTP_SERVICE);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    };

    public static class IsAmazonUrlConnectionHttpServicePresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName(URL_CONNECTION_HTTP_SERVICE);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    };

    public static class IsAmazonAwsCrtHttpServicePresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName(AWS_CRT_HTTP_SERVICE);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
