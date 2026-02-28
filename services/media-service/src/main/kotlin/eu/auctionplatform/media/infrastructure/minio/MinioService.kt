package eu.auctionplatform.media.infrastructure.minio

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URI
import java.time.Duration

/**
 * Service for interacting with MinIO (S3-compatible) object storage.
 *
 * Provides presigned URL generation for direct browser uploads/downloads,
 * object deletion, and object copying (used during image processing to
 * move originals to a processed bucket).
 *
 * Configuration is read from `application.yml` under the `minio.*` namespace.
 */
@ApplicationScoped
class MinioService(
    @ConfigProperty(name = "minio.endpoint")
    private val endpoint: String,

    @ConfigProperty(name = "minio.access-key")
    private val accessKey: String,

    @ConfigProperty(name = "minio.secret-key")
    private val secretKey: String,

    @ConfigProperty(name = "minio.region", defaultValue = "eu-west-1")
    private val region: String
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(MinioService::class.java)
    }

    private val credentials: StaticCredentialsProvider by lazy {
        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
    }

    private val s3Client: S3Client by lazy {
        S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(credentials)
            .region(Region.of(region))
            .forcePathStyle(true)
            .build()
    }

    private val presigner: S3Presigner by lazy {
        S3Presigner.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(credentials)
            .region(Region.of(region))
            .build()
    }

    /**
     * Generates a presigned URL for uploading an object directly to MinIO.
     *
     * The client uses this URL to PUT the file directly to object storage,
     * bypassing the application server for large file transfers.
     *
     * @param bucket      Target bucket name.
     * @param objectKey   The object key (path) within the bucket.
     * @param contentType Expected MIME type of the upload.
     * @param expiry      Duration the presigned URL remains valid.
     * @return The presigned upload URL as a string.
     */
    fun generatePresignedUploadUrl(
        bucket: String,
        objectKey: String,
        contentType: String,
        expiry: Duration = Duration.ofMinutes(15)
    ): String {
        LOG.debugf("Generating presigned upload URL: bucket=%s, key=%s, type=%s", bucket, objectKey, contentType)

        val putRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(contentType)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(expiry)
            .putObjectRequest(putRequest)
            .build()

        val presignedRequest = presigner.presignPutObject(presignRequest)
        return presignedRequest.url().toString()
    }

    /**
     * Generates a presigned URL for downloading (GET) an object from MinIO.
     *
     * @param bucket    Source bucket name.
     * @param objectKey The object key to download.
     * @param expiry    Duration the presigned URL remains valid.
     * @return The presigned download URL as a string.
     */
    fun generatePresignedDownloadUrl(
        bucket: String,
        objectKey: String,
        expiry: Duration = Duration.ofHours(1)
    ): String {
        LOG.debugf("Generating presigned download URL: bucket=%s, key=%s", bucket, objectKey)

        val getRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .build()

        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(expiry)
            .getObjectRequest(getRequest)
            .build()

        val presignedRequest = presigner.presignGetObject(presignRequest)
        return presignedRequest.url().toString()
    }

    /**
     * Deletes an object from MinIO.
     *
     * @param bucket    The bucket containing the object.
     * @param objectKey The object key to delete.
     */
    fun deleteObject(bucket: String, objectKey: String) {
        LOG.infof("Deleting object: bucket=%s, key=%s", bucket, objectKey)

        val deleteRequest = DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .build()

        s3Client.deleteObject(deleteRequest)
    }

    /**
     * Copies an object from one location to another within MinIO.
     *
     * Used by the image processing pipeline to move originals to a
     * processed bucket or to create derivative copies.
     *
     * @param sourceBucket The source bucket.
     * @param sourceKey    The source object key.
     * @param destBucket   The destination bucket.
     * @param destKey      The destination object key.
     */
    fun copyObject(sourceBucket: String, sourceKey: String, destBucket: String, destKey: String) {
        LOG.infof(
            "Copying object: %s/%s -> %s/%s",
            sourceBucket, sourceKey, destBucket, destKey
        )

        val copyRequest = CopyObjectRequest.builder()
            .sourceBucket(sourceBucket)
            .sourceKey(sourceKey)
            .destinationBucket(destBucket)
            .destinationKey(destKey)
            .build()

        s3Client.copyObject(copyRequest)
    }
}
