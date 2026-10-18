package com.ppms.shared.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(JwtKeyProvider.class)
public class JwtSecurityAutoConfiguration {
}
