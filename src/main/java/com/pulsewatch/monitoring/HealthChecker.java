package com.pulsewatch.monitoring;

import com.pulsewatch.entity.Monitor;

public interface HealthChecker {
    CheckExecutionResult check(Monitor monitor);
}
