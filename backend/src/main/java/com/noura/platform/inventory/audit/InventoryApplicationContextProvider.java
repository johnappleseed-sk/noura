package com.noura.platform.inventory.audit;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class InventoryApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        InventoryApplicationContextProvider.applicationContext = applicationContext;
    }

    public static <T> T getBean(Class<T> beanType) {
        return applicationContext.getBean(beanType);
    }
}
