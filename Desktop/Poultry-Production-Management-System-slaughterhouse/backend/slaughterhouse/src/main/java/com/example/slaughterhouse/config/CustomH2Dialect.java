package com.example.slaughterhouse.config;

import org.hibernate.dialect.H2Dialect;

/**
 * Custom H2 Dialect that disables RETURNING clause support
 * H2 doesn't support RETURNING in the same way as PostgreSQL
 */
public class CustomH2Dialect extends H2Dialect {

    @Override
    public boolean supportsInsertReturning() {
        return false;
    }

    @Override
    public boolean supportsInsertReturningGeneratedKeys() {
        return false;
    }
}