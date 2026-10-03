package com.example.erp.common.bean;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class MyBeanPostProcessor
                implements BeanPostProcessor {

        @Override
        public Object postProcessBeforeInitialization(
                        Object bean,
                        String beanName) {

                System.out.println(
                                "BEFORE: " + beanName);

                return bean;
        }

        @Override
        public Object postProcessAfterInitialization(
                        Object bean,
                        String beanName) {

                System.out.println(
                                "AFTER: " + beanName);

                return bean;
        }
}
