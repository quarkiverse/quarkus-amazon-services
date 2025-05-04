package io.quarkiverse.amazon.sso.runtime;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProviderFactory;
import software.amazon.awssdk.auth.credentials.internal.ProfileCredentialsUtils;
import software.amazon.awssdk.services.sso.auth.SsoProfileCredentialsProviderFactory;

public class SsoSubstitutions {

    @TargetClass(ProfileCredentialsUtils.class)
    static final class Target_ProfileCredentialsUtils {

        @Substitute
        private ProfileCredentialsProviderFactory ssoCredentialsProviderFactory() {
            return new SsoProfileCredentialsProviderFactory();
        }
    }
}
