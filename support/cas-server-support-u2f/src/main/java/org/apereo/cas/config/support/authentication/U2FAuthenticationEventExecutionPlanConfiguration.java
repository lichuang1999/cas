package org.apereo.cas.config.support.authentication;

import org.apereo.cas.adaptors.u2f.U2FAuthenticationHandler;
import org.apereo.cas.adaptors.u2f.U2FMultifactorAuthenticationProvider;
import org.apereo.cas.adaptors.u2f.storage.U2FDeviceRepository;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationMetaDataPopulator;
import org.apereo.cas.authentication.metadata.AuthenticationContextAttributeMetaDataPopulator;
import org.apereo.cas.authentication.principal.DefaultPrincipalFactory;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.mfa.MultifactorAuthenticationProperties;
import org.apereo.cas.services.DefaultMultifactorAuthenticationProviderBypass;
import org.apereo.cas.services.MultifactorAuthenticationProvider;
import org.apereo.cas.services.MultifactorAuthenticationProviderBypass;
import org.apereo.cas.services.ServicesManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * This is {@link U2FAuthenticationEventExecutionPlanConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@Configuration("u2fAuthenticationEventExecutionPlanConfiguration")
public class U2FAuthenticationEventExecutionPlanConfiguration implements AuthenticationEventExecutionPlanConfigurer {
    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    @Qualifier("servicesManager")
    private ServicesManager servicesManager;

    @Lazy
    @Autowired
    @Qualifier("u2fDeviceRepository")
    private U2FDeviceRepository u2fDeviceRepository;

    @Bean
    @RefreshScope
    public AuthenticationMetaDataPopulator u2fAuthenticationMetaDataPopulator() {
        final String authenticationContextAttribute = casProperties.getAuthn().getMfa().getAuthenticationContextAttribute();
        return new AuthenticationContextAttributeMetaDataPopulator(authenticationContextAttribute,
                u2fAuthenticationHandler(),
                u2fAuthenticationProvider());
    }

    @Bean
    @RefreshScope
    public MultifactorAuthenticationProviderBypass u2fBypassEvaluator() {
        return new DefaultMultifactorAuthenticationProviderBypass(casProperties.getAuthn().getMfa().getU2f().getBypass());
    }

    @ConditionalOnMissingBean(name = "u2fPrincipalFactory")
    @Bean
    public PrincipalFactory u2fPrincipalFactory() {
        return new DefaultPrincipalFactory();
    }

    @Bean
    @RefreshScope
    public U2FAuthenticationHandler u2fAuthenticationHandler() {
        final MultifactorAuthenticationProperties.U2F u2f = this.casProperties.getAuthn().getMfa().getU2f();
        return new U2FAuthenticationHandler(u2f.getName(), servicesManager, u2fPrincipalFactory(), u2fDeviceRepository);
    }

    @Bean
    @RefreshScope
    public MultifactorAuthenticationProvider u2fAuthenticationProvider() {
        final U2FMultifactorAuthenticationProvider p = new U2FMultifactorAuthenticationProvider();
        p.setBypassEvaluator(u2fBypassEvaluator());
        p.setGlobalFailureMode(casProperties.getAuthn().getMfa().getGlobalFailureMode());
        p.setOrder(casProperties.getAuthn().getMfa().getU2f().getRank());
        p.setId(casProperties.getAuthn().getMfa().getU2f().getId());
        return p;
    }

    @Override
    public void configureAuthenticationExecutionPlan(final AuthenticationEventExecutionPlan plan) {
        plan.registerAuthenticationHandler(u2fAuthenticationHandler());
        plan.registerMetadataPopulator(u2fAuthenticationMetaDataPopulator());
    }
}
