package com.dynamicdashboard.cockpit.identity.application;

/**
 * Thrown when a user cannot be deleted because they own resources that must be
 * reassigned or deleted first.
 *
 * Carries the counts so the controller can build a structured 409 response body
 * that the frontend renders as a specific French message instead of a generic error.
 */
public class UserDeletionBlockedException extends RuntimeException {

    private final long dashboardCount;
    private final long queryCount;

    public UserDeletionBlockedException(long dashboardCount, long queryCount) {
        super("User owns " + dashboardCount + " dashboard(s) and " + queryCount + " query/queries — cannot delete");
        this.dashboardCount = dashboardCount;
        this.queryCount     = queryCount;
    }

    public long getDashboardCount() { return dashboardCount; }
    public long getQueryCount()     { return queryCount; }
}
