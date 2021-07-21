/*
 * Copyright 2021 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.metric.collector.env;

import com.navercorp.pinpoint.common.server.util.ServerBootLogger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author minwoo.jung
 */
public class MetricEnvironmentApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    private final ServerBootLogger logger = ServerBootLogger.getLogger(getClass());

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        logger.info("onApplicationEvent(ApplicationEnvironmentPreparedEvent)");
        ConfigurableEnvironment environment = event.getEnvironment();
        SpringApplication springApplication = event.getSpringApplication();

        MetricEnvironmentPostProcessor profileEnvironment = new MetricEnvironmentPostProcessor();
        profileEnvironment.postProcessEnvironment(environment, springApplication);

    }

    /**
     * @see org.springframework.boot.context.logging.LoggingApplicationListener#DEFAULT_ORDER
     * @see ConfigFileApplicationListener#DEFAULT_ORDER
     */
    @Override
    public int getOrder() {
        return ConfigFileApplicationListener.DEFAULT_ORDER + 1;
    }
}