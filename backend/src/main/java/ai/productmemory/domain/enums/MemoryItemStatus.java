package ai.productmemory.domain.enums;

public enum MemoryItemStatus {
    /** Item is current and confirmed */
    FRESH,
    /** Item may be outdated — confidence lowered */
    STALE,
    /** Item is proposed from document ingestion, not yet verified */
    PROPOSED,
    /** Item has been superseded by a new version */
    SUPERSEDED,
    /** Item was explicitly removed from the memory graph */
    ARCHIVED
}
