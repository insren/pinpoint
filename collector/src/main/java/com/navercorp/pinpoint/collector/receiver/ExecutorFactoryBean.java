/*
 * Copyright 2018 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.collector.receiver;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.navercorp.pinpoint.collector.config.ExecutorProperties;
import com.navercorp.pinpoint.collector.monitor.BypassRunnableDecorator;
import com.navercorp.pinpoint.collector.monitor.CountingRejectedExecutionHandler;
import com.navercorp.pinpoint.collector.monitor.LoggingRejectedExecutionHandler;
import com.navercorp.pinpoint.collector.monitor.MonitoredThreadPoolExecutor;
import com.navercorp.pinpoint.collector.monitor.RejectedExecutionHandlerChain;
import com.navercorp.pinpoint.collector.monitor.RunnableDecorator;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Woonduk Kang(emeroad)
 */
public class ExecutorFactoryBean extends org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean {

    private int logRate = 100;
    private String beanName;

    private boolean enableMonitoring = false;

    private MetricRegistry registry;

    public ExecutorFactoryBean() {
    }


    @Override
    public void setBeanName(String name) {
        super.setBeanName(name);
        this.beanName = name;
    }

    @Override
    protected ThreadPoolExecutor createExecutor(int corePoolSize, int maxPoolSize, int keepAliveSeconds, BlockingQueue<Runnable> queue,
                                              ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        if (enableMonitoring) {
            return newMonitoredExecutorService(corePoolSize, maxPoolSize, keepAliveSeconds, queue, threadFactory, rejectedExecutionHandler);
        }

        return new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveSeconds, TimeUnit.MILLISECONDS, queue, threadFactory, rejectedExecutionHandler);
    }

    private ThreadPoolExecutor newMonitoredExecutorService(int corePoolSize, int maxPoolSize, int keepAliveSeconds, BlockingQueue<Runnable> queue,
                                                        ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

        rejectedExecutionHandler = wrapHandlerChain(rejectedExecutionHandler);

        RunnableDecorator runnableDecorator = new BypassRunnableDecorator(beanName);

        MonitoredThreadPoolExecutor monitoredThreadPoolExecutor = new MonitoredThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveSeconds, TimeUnit.MILLISECONDS,
                queue, threadFactory, rejectedExecutionHandler, runnableDecorator);

        Gauge<Long> submitGauge = () -> (long) monitoredThreadPoolExecutor.getSubmitCount();
        this.registry.register(MetricRegistry.name(beanName, "submitted"), submitGauge);

        Gauge<Long> runningGauge = () -> (long) monitoredThreadPoolExecutor.getActiveCount();
        this.registry.register(MetricRegistry.name(beanName, "running"), runningGauge);

        Gauge<Long> completedTaskGauge = () -> (long) monitoredThreadPoolExecutor.getCompletedTaskCount();
        this.registry.register(MetricRegistry.name(beanName, "completed"), completedTaskGauge);

        return monitoredThreadPoolExecutor;
    }

    private RejectedExecutionHandler wrapHandlerChain(RejectedExecutionHandler rejectedExecutionHandler) {

        RejectedExecutionHandlerChain.Builder builder = new RejectedExecutionHandlerChain.Builder();
        if (registry != null) {
            RejectedExecutionHandler countingHandler = new CountingRejectedExecutionHandler(beanName, registry);
            builder.addRejectHandler(countingHandler);
        }

        if (logRate > -1) {
            RejectedExecutionHandler loggingHandler = new LoggingRejectedExecutionHandler(beanName, logRate);
            builder.addRejectHandler(loggingHandler);
        }

        // original exception policy
        builder.addRejectHandler(rejectedExecutionHandler);

        return builder.build();
    }


    public void setExecutorProperties(ExecutorProperties executorProperties) {
        setCorePoolSize(executorProperties.getThreadSize());
        setMaxPoolSize(executorProperties.getThreadSize());
        setQueueCapacity(executorProperties.getQueueSize());
        this.enableMonitoring = executorProperties.isMonitorEnable();
    }

    public void setRegistry(MetricRegistry registry) {
        this.registry = registry;
    }

    public void setLogRate(int logRate) {
        this.logRate = logRate;
    }

}
