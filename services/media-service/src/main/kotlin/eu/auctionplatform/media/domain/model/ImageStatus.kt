package eu.auctionplatform.media.domain.model

/**
 * Lifecycle status of a media image within the processing pipeline.
 *
 * Transitions:
 * - UPLOADING -> PROCESSING (after MinIO bucket notification)
 * - PROCESSING -> READY (after thumbnails generated, WebP conversion)
 * - PROCESSING -> FAILED (on any processing error)
 * - UPLOADING -> FAILED (if upload never completes / times out)
 */
enum class ImageStatus {

    /** Image upload initiated; presigned URL has been issued. */
    UPLOADING,

    /** Image received in object storage; processing pipeline running. */
    PROCESSING,

    /** All processing complete; thumbnails and optimised formats available. */
    READY,

    /** Processing failed; see error details in the image record. */
    FAILED
}
