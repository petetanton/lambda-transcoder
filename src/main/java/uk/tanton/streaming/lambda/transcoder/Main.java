package uk.tanton.streaming.lambda.transcoder;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.gson.Gson;
import uk.tanton.streaming.lambda.transcoder.domain.Profile;
import uk.tanton.streaming.lambda.transcoder.domain.TranscodeMessage;

import java.io.IOException;
import java.util.Optional;

public class Main implements RequestHandler<SNSEvent, String> {

    private final Gson gson;
    private final AmazonS3Client amazonS3Client;
    private final S3FileManager s3FileManager;

    public Main() throws IOException {
        this.gson = new Gson();
        this.amazonS3Client = new AmazonS3Client();
        this.s3FileManager = new S3FileManager(this.amazonS3Client);
        prepareFfmpeg();
    }

//    Testing Constructor
    public Main(S3FileManager s3FileManager) throws IOException {
        this.s3FileManager = s3FileManager;
        this.gson = new Gson();
        this.amazonS3Client = new AmazonS3Client();
        prepareFfmpeg();
    }

    @Override
    public String handleRequest(SNSEvent snsEvent, Context context) {
        for (SNSEvent.SNSRecord record : snsEvent.getRecords()) {
            final TranscodeMessage transcodeMessage = gson.fromJson(record.getSNS().getMessage(), TranscodeMessage.class);
            context.getLogger().log(transcodeMessage.toString());

            try {
                this.s3FileManager.downloadObject(transcodeMessage.getBucket(), transcodeMessage.getKey(), context.getLogger());
            } catch (Exception e) {
                throw new TranscodeException("error when downloading object from S3", e);
            }


            context.getLogger().log(transcodeMessage.getKey());
            try {
                final String outputFile = transcode(transcodeMessage.getKey(), transcodeMessage.getProfile(), transcodeMessage.getStreamId(), context.getLogger());
                context.getLogger().log(String.format("output file: %s", outputFile));
                this.s3FileManager.uploadToS3(transcodeMessage, outputFile);
            } catch (IOException | InterruptedException e) {
                context.getLogger().log(e.getMessage());
                throw new TranscodeException(e);
            }
        }
        return "finished";
    }

//    private void convertStreamToFile(final S3Object object, final LambdaLogger logger) throws IOException, InterruptedException {
//        logger.log("Writing s3 object to file");
//        InputStream in = object.getObjectContent();
//        byte[] buf = new byte[1024];
//        OutputStream out = new FileOutputStream("/tmp/" + object.getKey().substring(object.getKey().lastIndexOf('/') + 1));
//        int count;
//        while ((count = in.read(buf)) != -1) {
//            if (Thread.interrupted()) {
//                out.close();
//                throw new InterruptedException();
//            }
//            out.write(buf, 0, count);
//        }
//        out.close();
//        in.close();
//    }

    private String transcode(final String key, final Profile profile, final String streamId, final LambdaLogger logger) throws IOException, InterruptedException {
        final Transcoder transcoder = new Transcoder("/tmp/ffmpeg", logger);

        final String outputFileName = String.format("/tmp/%s-%s-%s", streamId, profile.getProfileName(), key.substring(key.lastIndexOf('/') + 1));

        return transcoder.transcode(profile, "/tmp/" + key.substring(key.lastIndexOf('/') + 1), outputFileName);
    }

    private void prepareFfmpeg() throws IOException {
        commandRunner("mkdir -p /tmp", Optional.empty());
        commandRunner("cp /var/task/ffmpeg /tmp/ffmpeg", Optional.empty());
        commandRunner("chmod 755 /tmp/ffmpeg", Optional.empty());
    }


    private void commandRunner(final String cmd, final Optional<LambdaLogger> logger) throws IOException {
        if (logger.isPresent()) {
            logger.get().log("Running: " + cmd);
        } else {
            System.out.println("Running: " + cmd);  // NOSONAR
        }
        final Process start = Runtime.getRuntime().exec(cmd);
        final StreamConsumer error = new StreamConsumer(start.getErrorStream(), "ERROR", logger);
        final StreamConsumer info = new StreamConsumer(start.getInputStream(), "INFO", logger);
        error.start();
        info.start();

        while (start.isAlive() || error.isAlive() || info.isAlive()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {    // NOSONAR
            }
        }
    }



//    private void uploadToS3(final TranscodeMessage transcodeMessage, final String filename) {
//        final String key = String.format("hls/%s/%s", transcodeMessage.getStreamId(), filename.substring(filename.lastIndexOf('/') + 1));
//        final PutObjectRequest putObjectRequest = new PutObjectRequest("live-streaming-shared-livestreamingdistribution-199wt2f91f0ik", key, new File(filename));
//        this.amazonS3Client.putObject(putObjectRequest);
//    }
}
