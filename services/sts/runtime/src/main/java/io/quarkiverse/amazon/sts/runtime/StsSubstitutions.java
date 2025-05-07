package io.quarkiverse.amazon.sts.runtime;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import software.amazon.awssdk.auth.credentials.ChildProfileCredentialsProviderFactory;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenCredentialsProviderFactory;
import software.amazon.awssdk.auth.credentials.internal.ProfileCredentialsUtils;
import software.amazon.awssdk.auth.credentials.internal.WebIdentityCredentialsUtils;
import software.amazon.awssdk.services.sts.internal.StsProfileCredentialsProviderFactory;
import software.amazon.awssdk.services.sts.internal.StsWebIdentityCredentialsProviderFactory;

public class StsSubstitutions {

    @TargetClass(WebIdentityCredentialsUtils.class)
    static final class Target_WebIdentityCredentialsUtils {

        @Substitute
        public static WebIdentityTokenCredentialsProviderFactory factory() {
            return new StsWebIdentityCredentialsProviderFactory();
        }
    }

    @TargetClass(ProfileCredentialsUtils.class)
    static final class Target_ProfileCredentialsUtils {

        @Substitute
        private ChildProfileCredentialsProviderFactory stsCredentialsProviderFactory() {
            return new StsProfileCredentialsProviderFactory();
        }
    }
}
