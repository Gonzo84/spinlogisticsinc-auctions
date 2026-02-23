package eu.auctionplatform.commons.util

import java.security.SecureRandom
import java.util.UUID

/**
 * Generator for UUIDv7 identifiers (RFC 9562).
 *
 * UUIDv7 encodes a Unix-epoch millisecond timestamp in the most-significant 48 bits,
 * making the identifiers time-sortable while retaining global uniqueness via random
 * entropy in the remaining bits.
 *
 * Layout (128 bits total):
 *   bits  0..47  – Unix timestamp in milliseconds
 *   bits 48..51  – version  (0b0111 = 7)
 *   bits 52..63  – 12 bits of random data (rand_a)
 *   bits 64..65  – variant  (0b10)
 *   bits 66..127 – 62 bits of random data (rand_b)
 */
object IdGenerator {

    private val random: SecureRandom = SecureRandom()

    /**
     * Generates a new UUIDv7.
     */
    fun generateUUIDv7(): UUID {
        return generateUUIDv7(System.currentTimeMillis())
    }

    /**
     * Generates a UUIDv7 seeded with the given [timestampMillis].
     * Visible for deterministic testing.
     */
    internal fun generateUUIDv7(timestampMillis: Long): UUID {
        // --- most significant 64 bits ---
        // bits 0..47  : timestamp
        // bits 48..51 : version 7 (0b0111)
        // bits 52..63 : 12 random bits
        val randA = random.nextInt() and 0x0FFF // 12 bits
        val msb: Long =
            (timestampMillis shl 16)          // timestamp in top 48 bits
                .or(7L shl 12)                // version nibble
                .or(randA.toLong())           // 12 random bits

        // --- least significant 64 bits ---
        // bits 64..65 : variant 10
        // bits 66..127: 62 random bits
        val randB = random.nextLong()
        val lsb: Long =
            (randB and 0x3FFF_FFFF_FFFF_FFFFL) // clear top 2 bits
                .or(0x8000_0000_0000_0000UL.toLong()) // set variant to 10

        return UUID(msb, lsb)
    }

    /**
     * Convenience: returns the UUIDv7 as a lowercase string.
     */
    fun generateString(): String = generateUUIDv7().toString()
}
