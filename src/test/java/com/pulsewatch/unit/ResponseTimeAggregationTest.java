package com.pulsewatch.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ResponseTimeAggregationTest {

    @Test
    @DisplayName("Calculate min, max, avg, median, and p95 from latency samples")
    void testLatencyAggregation() {
        List<Long> latencies = Arrays.asList(45L, 50L, 55L, 60L, 70L, 85L, 95L, 120L, 200L, 500L);
        Collections.sort(latencies);

        long min = latencies.get(0);
        long max = latencies.get(latencies.size() - 1);
        long sum = latencies.stream().mapToLong(Long::longValue).sum();
        long avg = sum / latencies.size();
        long median = latencies.get(latencies.size() / 2);
        int p95Index = (int) Math.min(latencies.size() - 1, Math.round(latencies.size() * 0.95));
        long p95 = latencies.get(p95Index);

        assertEquals(45L, min);
        assertEquals(500L, max);
        assertEquals(128L, avg);
        assertEquals(85L, median);
        assertEquals(500L, p95);
    }
}
