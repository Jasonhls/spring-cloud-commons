/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.context.properties;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Collects references to <code>@ConfigurationProperties</code> beans in the context and
 * its parent.
 *
 * @author Dave Syer
 */
@Component
public class ConfigurationPropertiesBeans
		implements BeanPostProcessor, ApplicationContextAware {

	//添加有@ConfiguraitonProperties注解的bean都将保存在该集合中
	private Map<String, ConfigurationPropertiesBean> beans = new HashMap<>();

	private ApplicationContext applicationContext;

	private ConfigurableListableBeanFactory beanFactory;

	private String refreshScope;

	private boolean refreshScopeInitialized;

	private ConfigurationPropertiesBeans parent;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
		if (applicationContext
				.getAutowireCapableBeanFactory() instanceof ConfigurableListableBeanFactory) {
			this.beanFactory = (ConfigurableListableBeanFactory) applicationContext
					.getAutowireCapableBeanFactory();
		}
		if (applicationContext.getParent() != null && applicationContext.getParent()
				.getAutowireCapableBeanFactory() instanceof ConfigurableListableBeanFactory) {
			ConfigurableListableBeanFactory listable = (ConfigurableListableBeanFactory) applicationContext
					.getParent().getAutowireCapableBeanFactory();
			String[] names = listable
					.getBeanNamesForType(ConfigurationPropertiesBeans.class);
			if (names.length == 1) {
				this.parent = (ConfigurationPropertiesBeans) listable.getBean(names[0]);
				this.beans.putAll(this.parent.beans);
			}
		}
	}

	/**
	 * @param beans The bean meta data to set.
	 * @deprecated since 2.2.0 because
	 * {@link org.springframework.boot.context.properties.ConfigurationBeanFactoryMetadata}
	 * has been deprecated
	 */
	@Deprecated
	public void setBeanMetaDataStore(
			org.springframework.boot.context.properties.ConfigurationBeanFactoryMetadata beans) {
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		//遍历注册的所有Scope并且判断是否有RefreshScope，先从注册的所有Scope中查找RefreshScope，如果没有返回false，如果是返回true
		if (isRefreshScoped(beanName)) {
			return bean;
		}
		/**
		 * 如果isRefreshScope返回false就判断当前Bean是否有@ConfigurationProperties注解，如果有会被包装成ConfigurationPropertiesBean，
		 * 并存入到this.beans集合中
		 */
		ConfigurationPropertiesBean propertiesBean = ConfigurationPropertiesBean
				.get(this.applicationContext, bean, beanName);
		if (propertiesBean != null) {
			this.beans.put(beanName, propertiesBean);
		}
		return bean;
	}

	private boolean isRefreshScoped(String beanName) {
		if (this.refreshScope == null && !this.refreshScopeInitialized) {
			this.refreshScopeInitialized = true;
			for (String scope : this.beanFactory.getRegisteredScopeNames()) {
				if (this.beanFactory.getRegisteredScope(
						scope) instanceof org.springframework.cloud.context.scope.refresh.RefreshScope) {
					this.refreshScope = scope;
					break;
				}
			}
		}
		if (beanName == null || this.refreshScope == null) {
			return false;
		}
		return this.beanFactory.containsBeanDefinition(beanName) && this.refreshScope
				.equals(this.beanFactory.getBeanDefinition(beanName).getScope());
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	public Set<String> getBeanNames() {
		//返回所有带@ConfigurationProperties注解的bean的名称集合
		return new HashSet<String>(this.beans.keySet());
	}

}
