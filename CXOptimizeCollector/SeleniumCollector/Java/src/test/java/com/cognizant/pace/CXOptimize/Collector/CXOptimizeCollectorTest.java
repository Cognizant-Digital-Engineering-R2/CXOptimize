package com.cognizant.pace.CXOptimize.Collector;


import io.specto.hoverfly.junit.rule.HoverflyRule;

import static io.specto.hoverfly.junit.core.HoverflyConfig.configs;
import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import org.junit.ClassRule;
import org.junit.Test;

import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.junit.Assert.assertEquals;

import org.springframework.web.client.RestTemplate;

import org.springframework.http.ResponseEntity;




public class CXOptimizeCollectorTest {

/*
    @ClassRule
   // public static HoverflyRule hoverflyRule = HoverflyRule.inSimulationMode(dsl(service("http://localhost/").get("/getConfig").willReturn(success("{\"bookingId\":\"1\"}", "application/json"))));
    //@ClassRule
    public static HoverflyRule hoverflyRule = HoverflyRule.inSimulationMode(dsl(service("http://localhost").get("/getConfig").willReturn(success("{\"bookingId\":\"1\"}", "application/json"))),configs().proxyLocalHost());
    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    public void testAFirstStub() {
        final ResponseEntity<String> courseResponse = restTemplate.getForEntity("http://localhost/getConfig", String.class);
        assertEquals("200", courseResponse.getStatusCode().toString());
        assertEquals("{\"bookingId\":\"1\"}", courseResponse.getBody());
    }*/

    @Test
    public void startTransaction()
    {

    }

    @Test
    public void endTransaction() {
    }

    @Test
    public void chromeTest()
    {

    }
}