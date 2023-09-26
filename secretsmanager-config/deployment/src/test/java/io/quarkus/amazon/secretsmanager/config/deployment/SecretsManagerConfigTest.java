package io.quarkus.amazon.secretsmanager.config.deployment;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SecretsManagerConfigTest {

    @ConfigProperty(name = "mysecretkey")
    String secret;

    @ConfigProperty(name = "myprefix.mysecretkey")
    String secretWithPrefix;

    @ConfigProperty(name = "otherprefix.mysecretkey")
    String secretWithOtherPrefix;

    @ConfigProperty(name = "disabledprefix.mysecretkey", defaultValue = "unset")
    String secretWithDisabledPrefix;

    @ConfigProperty(name = "othersecretkey", defaultValue = "unset")
    String unset;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("devservices.properties", "application.properties"));

    @Test
    public void test() {
        Assert.assertEquals("mysecretvalue", secret);
        Assert.assertEquals("mysecretvalue", secretWithPrefix);
        Assert.assertEquals("mysecretvalue", secretWithOtherPrefix);
        Assert.assertEquals("unset", secretWithDisabledPrefix);
        Assert.assertEquals("unset", unset);
    }
}
