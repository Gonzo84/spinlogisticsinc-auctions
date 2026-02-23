package eu.auctionplatform.commons.domain

/**
 * Base class for value objects.
 *
 * Value objects are immutable types that are defined entirely by their attributes
 * rather than by an identity. Two value objects are equal when all of their
 * constituent properties are equal.
 *
 * Subclasses **must** override [equals] and [hashCode] (enforced by the abstract
 * declarations below). Kotlin data classes satisfy this contract automatically,
 * but non-data subclasses must provide their own implementations.
 */
abstract class ValueObject {

    /**
     * Returns the properties that participate in equality comparison.
     * Subclasses should return all constituent fields in a list.
     */
    protected abstract fun equalityComponents(): List<Any?>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as ValueObject
        return equalityComponents() == other.equalityComponents()
    }

    override fun hashCode(): Int {
        return equalityComponents().fold(17) { acc, component ->
            31 * acc + (component?.hashCode() ?: 0)
        }
    }

    override fun toString(): String {
        return "${this::class.simpleName}(${equalityComponents().joinToString(", ")})"
    }
}
