package me.chrr.camerapture.domain.registry;

/** Structured outcome of safe automatic entity valuation. */
public enum AutomaticValueStatus {
    AVAILABLE,
    NOT_LIVING,
    NO_DEFAULT_ATTRIBUTES,
    MISSING_ATTRIBUTE,
    READ_ERROR
}
