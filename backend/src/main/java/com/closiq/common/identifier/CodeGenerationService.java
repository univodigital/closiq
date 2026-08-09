package com.closiq.common.identifier;

/**
 * Common contract for human-readable business identifiers.
 * Implementations may be swapped (DB sequence, Redis, Snowflake, ULID, etc.)
 * without changing domain services.
 */
public interface CodeGenerationService {

    String nextCode();
}
