package com.zaphira.common.test;

import org.hibernate.dialect.H2Dialect;

/**
 * H2 Dialect for testing purposes (SQLite replacement).
 * 
 * Uses Hibernate's H2Dialect to enable in-memory H2 database support.
 * H2 is used instead of SQLite for better test compatibility.
 * 
 * Configuration:
 * spring.jpa.database-platform: org.hibernate.dialect.H2Dialect
 * 
 * @since 1.0.0
 */
public class TestSQLiteDialect extends H2Dialect {
    // No additional implementation needed - H2Dialect has full H2 support
}
