package com.dynamicdashboard.cockpit.shared.domain;
public final class DomainEnums {
    private DomainEnums() {
    }
    public enum AccountStatus {
        ACTIVE, LOCKED, DISABLED, PENDING
    }
    public enum AssignmentScope {
        GLOBAL, ORGANIZATION, GROUP
    }
    public enum MembershipRole {
        OWNER, ADMIN, MEMBER, READER
    }
    public enum FieldType {
        TEXT, NUMBER, DATE, AMOUNT, PERCENT
    }
    public enum QueryVisibility {
        PERSONAL, SHARED
    }
    public enum AggregationType {
        NONE, SUM, AVG, MIN, MAX, COUNT
    }
    public enum QueryJoinType {
        INNER, LEFT, RIGHT, FULL
    }
    public enum FilterOperator {
        EQ, NEQ, GT, LT, CONTAINS, IN, BETWEEN
    }
    public enum LogicalOperator {
        AND, OR
    }
    public enum QueryTransformationType {
        RENAME, CALCULATED, FORMAT, REPLACE_EMPTY, FILTER_ROWS
    }
    public enum SortDirection {
        ASC, DESC
    }
    public enum DashboardStatus {
        DRAFT, PUBLISHED
    }
    public enum ShareLevel {
        PRIVATE, USERS, GROUP, ORGANIZATION
    }
    public enum DashboardDensity {
        COMPACT, NORMAL, AIRY
    }
    public enum RefreshInterval {
        OFF, S5, S10, S30, M1, M5, M15, M30, H1, INHERIT
    }
    public enum WidgetType {
        KPI, BAR, STACKED, LINE, PIE, DONUT, HEATMAP, GAUGE, DATAGRID, TEXT
    }
    public enum KpiFormat {
        AMOUNT, PERCENT, INTEGER
    }
    public enum DataGridDensity {
        COMPACT, NORMAL, COMFORTABLE
    }
    public enum GlobalFilterInput {
        SELECT, DATE, DATERANGE, TEXT, MULTISELECT
    }
    public enum AccessLevel {
        READ, EDIT, OWNER
    }
    public enum AlertStatus {
        ACTIVE, ACKNOWLEDGED, SNOOZED, RESOLVED
    }

    public enum AlertSeverity {
        INFO, WARNING, CRITICAL
    }

    public enum AlertMetric {
        TOTAL, AVERAGE, MAXIMUM, MINIMUM
    }

    public enum AlertComparisonOperator {
        GT, LT, EQ
    }

    public enum AlertChannel {
        IN_APP, EMAIL, SMS, WHATSAPP
    }

    public enum AnalyticsAction {
        DASHBOARD_VIEW, DASHBOARD_IMPRESSION, DASHBOARD_CLICK,
        WIDGET_IMPRESSION, WIDGET_INTERACTION,
        RAW_DATA_VIEW, RAW_DATA_EXPORT,
        QUERY_EXECUTION
    }

    public enum AnalyticsTarget {
        DASHBOARD, WIDGET, QUERY, ALERT_RULE, ALERT_EVENT, DATA_SOURCE
    }

}
