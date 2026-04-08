package ai.productmemory.domain.enums;

public enum MemoryLinkType {
    /** A capability uses a screen to expose functionality */
    CAPABILITY_USES_SCREEN,
    /** A screen calls an API endpoint */
    SCREEN_CALLS_API,
    /** An API manipulates a data entity */
    API_USES_DATA_ENTITY,
    /** A decision drives the shape of a capability */
    DECISION_GOVERNS_CAPABILITY,
    /** A requirement is fulfilled by a capability */
    REQUIREMENT_FULFILLED_BY,
    /** A capability depends on another capability */
    DEPENDS_ON,
    /** General related-to link */
    RELATED_TO,
    /** Evidence confirms a memory item */
    EVIDENCE_CONFIRMS,
    /** Change request impacts a memory item */
    CR_IMPACTS
}
