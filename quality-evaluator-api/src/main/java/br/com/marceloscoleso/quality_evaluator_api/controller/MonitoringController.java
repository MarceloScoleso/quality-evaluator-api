package br.com.marceloscoleso.quality_evaluator_api.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class MonitoringController {

    private final HealthEndpoint healthEndpoint;
    private final InfoEndpoint infoEndpoint;
    private final MeterRegistry meterRegistry;

    private final String[] customMetrics = {
            "business.evaluations.create.time",
            "business.evaluations.created",
            "business.evaluations.not_found",
            "business.projects.create.time",
            "business.projects.created",
            "business.projects.listed",
            "business.projects.not_found"
    };

    public MonitoringController(
            HealthEndpoint healthEndpoint,
            InfoEndpoint infoEndpoint,
            MeterRegistry meterRegistry
    ) {
        this.healthEndpoint = healthEndpoint;
        this.infoEndpoint = infoEndpoint;
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/health")
    public String health(Model model) {
        HealthComponent healthComponent = healthEndpoint.health();
        model.addAttribute("status", healthComponent.getStatus());
        if (healthComponent instanceof org.springframework.boot.actuate.health.CompositeHealth composite) {
            model.addAttribute("components", composite.getComponents());
        }
        return "health";
    }

    @GetMapping("/info")
    public String info(Model model) {
        model.addAttribute("info", infoEndpoint.info());
        return "info";
    }

    @GetMapping("/metrics")
    public String metrics(Model model) {

        Map<String, Object> metricsWithValues = new LinkedHashMap<>();

        meterRegistry.getMeters().forEach(meter -> {
            String name = meter.getId().getName();
            double value = 0.0;

            if (meter instanceof Counter counter) {
                value = counter.count();
            } else if (meter instanceof Timer timer) {
                value = timer.count(); 
            } else if (meter.measure().iterator().hasNext()) {
                value = meter.measure().iterator().next().getValue();
            }

            metricsWithValues.put(name, value);
        });

        
        for (String metric : customMetrics) {
            metricsWithValues.putIfAbsent(metric, 0.0);
        }

        model.addAttribute("metrics", metricsWithValues);
        return "metrics";
    }
}
