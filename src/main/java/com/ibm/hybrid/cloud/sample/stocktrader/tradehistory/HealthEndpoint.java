package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

@Health
@ApplicationScoped
public class HealthEndpoint implements HealthCheck {
    private static final long serialVersionUID = 1L;

    @Override
    public HealthCheckResponse call() {
        //TODO: add checks for mongo, kafka, etc
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Consumer");

        return builder.up().build();
    }

}