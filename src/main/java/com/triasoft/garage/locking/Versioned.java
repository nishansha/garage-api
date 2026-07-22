package com.triasoft.garage.locking;

/**
 * Implemented by update-request DTOs that carry the optimistic-lock version the
 * client last read. {@link VersionCheck} reads it via this interface to detect
 * stale-screen edits.
 */
public interface Versioned {

    Long getVersion();
}
