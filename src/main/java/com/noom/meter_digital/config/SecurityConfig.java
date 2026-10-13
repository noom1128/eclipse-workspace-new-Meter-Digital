package com.noom.meter_digital.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.noom.meter_digital.security.JwtAuthenticationFilter;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
	
	@Autowired
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	public AuthenticationManager authenticationManager(
	        AuthenticationConfiguration config) throws Exception {
	    return config.getAuthenticationManager();
	}
	
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/login", "/billing", "/download", "/download.html", "/landing", "/store", "/product", "/old-dashboard", "/live-dashboard", "/maintenance", "/meter-gateway-setup", "/gateway-setup", "/config", "/meter-config", "/meter-reader", "/modbus-debugger", "/modbus-remote-tool.html", "/error", "/dashboard.html", "/css/**", "/js/**", "/api/auth/**", "/api/download/**", "/api/license/**", "/api/maintenance/**", "/api/v1/meters/**", "/api/v1/meter-configs/**", "/api/meter-configs/**", "/api/v1/modbus/**", "/api/modbus/**", "/api/v1/modbus-debug/**", "/api/modbus-debug/**", "/api/meter/image/**", "/ws/**", "/ws-raw/**").permitAll()

            .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/settings/promptpay-qr").permitAll()
            .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/meter", "/api/meter/ocr-scan", "/api/meter/ocr-scan-cloud", "/api/meter/submit-photo", "/api/meter/approve-pending/**", "/api/meter/line/**", "/api/line/**").permitAll()
            .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/meter", "/api/meter/latest", "/api/meter/network-mode", "/api/meter/pending-photos", "/api/meter/history", "/api/meter/check-anomaly").permitAll()
            .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/rooms/**").permitAll()
            .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/rooms/**").hasRole("ADMIN")
            .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/rooms/**").hasRole("ADMIN")
            .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/rooms/**").hasRole("ADMIN")
            .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/meter/**").hasRole("ADMIN")
            .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/meter/**").hasRole("ADMIN")
            .requestMatchers("/api/billing/**").hasRole("ADMIN")
            .requestMatchers("/api/meter/latest-by-room").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .formLogin(form -> form.disable())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
	}
}
