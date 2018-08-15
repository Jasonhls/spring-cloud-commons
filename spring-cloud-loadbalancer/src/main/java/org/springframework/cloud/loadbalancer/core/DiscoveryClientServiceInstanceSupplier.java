/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.loadbalancer.core;

import java.util.List;

import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;

import static org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory.PROPERTY_NAME;

/**
 * @author Spencer Gibb
 */
public class DiscoveryClientServiceInstanceSupplier implements ServiceInstanceSupplier {

	private final DiscoveryClient delegate;
	private final String serviceId;

	public DiscoveryClientServiceInstanceSupplier(DiscoveryClient delegate, Environment environment) {
		this.delegate = delegate;
		serviceId = environment.getProperty(PROPERTY_NAME);
	}

	@Override
	public Flux<ServiceInstance> get() {
		List<ServiceInstance> instances = delegate.getInstances(serviceId);
		return Flux.fromIterable(instances);
	}

	public String getServiceId() {
		return this.serviceId;
	}
}
