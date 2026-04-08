package ai.productmemory.domain.enums;

public enum ExecutionRunStatus {
    QUEUED,
    INITIALIZING,
    RUNNING,
    COMPLETED,
    FAILED,
    TIMED_OUT,
    CANCELLED
}
