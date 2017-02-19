package uk.tanton.streaming.lambda.transcoder;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import uk.tanton.streaming.lambda.transcoder.domain.TranscodeMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class S3FileManager {

    private final AmazonS3Client amazonS3Client;

    public S3FileManager(final AmazonS3Client amazonS3Client) {
        this.amazonS3Client = amazonS3Client;
    }

    public void downloadObject(final String bucket, final String key, final LambdaLogger logger) throws IOException, InterruptedException {
        final S3Object object = this.amazonS3Client.getObject(bucket, key);

        logger.log("Writing s3 object to file");
        InputStream in = object.getObjectContent();
        byte[] buf = new byte[1024];
        OutputStream out = new FileOutputStream("/tmp/" + object.getKey().substring(object.getKey().lastIndexOf('/') + 1));
        int count;
        while ((count = in.read(buf)) != -1) {
            if (Thread.interrupted()) {
                out.close();
                throw new InterruptedException();
            }
            out.write(buf, 0, count);
        }
        out.close();
        in.close();
    }

    public void uploadToS3(final TranscodeMessage transcodeMessage, final String filename) {
        final String key = String.format("hls/%s/%s", transcodeMessage.getStreamId(), filename.substring(filename.lastIndexOf('/') + 1));
        final PutObjectRequest putObjectRequest = new PutObjectRequest("live-streaming-shared-livestreamingdistribution-199wt2f91f0ik", key, new File(filename));
        this.amazonS3Client.putObject(putObjectRequest);
    }
}
