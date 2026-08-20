package com.example.orm.jpa.config;

import com.blazebit.persistence.Criteria;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.integration.view.spring.EnableEntityViews;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.spi.EntityViewConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for Blaze-Persistence entity views.
 */
@Configuration
@EnableEntityViews(basePackages = "com.example.orm.jpa.view")
public class BlazeConfig {

    @Bean
    public CriteriaBuilderFactory criteriaBuilderFactory(EntityManagerFactory emf) {
        return Criteria.getDefault().createCriteriaBuilderFactory(emf);
    }

    @Bean
    public EntityViewManager entityViewManager(CriteriaBuilderFactory cbf, EntityViewConfiguration config) {
        return config.createEntityViewManager(cbf);
    }
}
