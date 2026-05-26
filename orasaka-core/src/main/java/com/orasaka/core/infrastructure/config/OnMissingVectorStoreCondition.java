package com.orasaka.core.infrastructure.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Condition that matches when no VectorStore bean is defined in the application context. */
public class OnMissingVectorStoreCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    if (context.getBeanFactory() == null) {
      return true;
    }
    try {
      Class<?> vectorStoreClass = Class.forName("org.springframework.ai.vectorstore.VectorStore");
      String[] beanNames = context.getBeanFactory().getBeanNamesForType(vectorStoreClass);
      return beanNames.length == 0;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
