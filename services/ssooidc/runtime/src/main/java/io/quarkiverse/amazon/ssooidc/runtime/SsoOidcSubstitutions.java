package io.quarkiverse.amazon.ssooidc.runtime;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import software.amazon.awssdk.auth.token.credentials.ChildProfileTokenProviderFactory;
import software.amazon.awssdk.auth.token.internal.ProfileTokenProviderLoader;
import software.amazon.awssdk.services.ssooidc.SsoOidcProfileTokenProviderFactory;

public class SsoOidcSubstitutions {

    @TargetClass(ProfileTokenProviderLoader.class)
    static final class Target_ProfileTokenProviderLoader {

        @Substitute
        private ChildProfileTokenProviderFactory ssoTokenProviderFactory() {
            return new SsoOidcProfileTokenProviderFactory();
        }
    }
}
