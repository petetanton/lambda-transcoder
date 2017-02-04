package uk.tanton.streaming.lambda.transcoder;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.gson.Gson;
import uk.tanton.streaming.lambda.transcoder.domain.Profile;
import uk.tanton.streaming.lambda.transcoder.domain.TranscodeMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

public class Main implements RequestHandler<SNSEvent, String> {

    private final Gson gson;
    private final AmazonS3Client amazonS3Client;

    public Main() throws IOException {
        this.gson = new Gson();
        this.amazonS3Client = new AmazonS3Client();
        prepareFfmpeg();
    }

    @Override
    public String handleRequest(SNSEvent snsEvent, Context context) {
        for (SNSEvent.SNSRecord record : snsEvent.getRecords()) {
            final TranscodeMessage transcodeMessage = gson.fromJson(record.getSNS().getMessage(), TranscodeMessage.class);
            context.getLogger().log(transcodeMessage.toString());
            final S3Object object = this.amazonS3Client.getObject(transcodeMessage.getBucket(), transcodeMessage.getKey());

            try {
                convertStreamToFile(object, context.getLogger());
            } catch (Exception e) {
                throw new TranscodeException(e);
            }


            context.getLogger().log(object.getKey());
            try {
                final String outputFile = transcode(transcodeMessage.getKey(), transcodeMessage.getProfile(), transcodeMessage.getStreamId(), context.getLogger());
                context.getLogger().log(String.format("output file: %s", outputFile));
                uploadToS3(transcodeMessage, outputFile);
            } catch (IOException | InterruptedException e) {
                context.getLogger().log(e.getMessage());
                throw new TranscodeException(e);
            }
        }
        return "finished";
    }

    private void convertStreamToFile(final S3Object object, final LambdaLogger logger) throws IOException, InterruptedException {
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

    private String transcode(final String key, final Profile profile, final String streamId, final LambdaLogger logger) throws IOException, InterruptedException {
        final String outputFileName = String.format("/tmp/%s-%s.ts", streamId, profile.getProfileName());
        final StringBuilder sb = new StringBuilder();
//         ffmpeg -i 1000ktest-5-100000.ts -vcodec libx264 -x264opts keyint=25:min-keyint=25:scenecut=-1 -b:v 500k -acodec copy test.ts

        final String ffmpeg = "/tmp/ffmpeg";
        logger.log("ffmpeg is at: " + ffmpeg);
        sb.append(ffmpeg).append(" -i ").append("/tmp/").append(key.substring(key.lastIndexOf('/') + 1))
                .append(" -vcodec ").append(profile.getVideoCodec())
                .append(" -x264opts ").append(profile.getX264Opts())
                .append(" -b:v ").append(profile.getVideoBitrate());
        if (profile.getHeight() > 0 && profile.getWidth() > 0) {
            sb.append(" -vf scale=320:240");
        }
        sb.append(" -acodec copy")
                .append(" -threads 0 -preset superfast")
                .append(" ").append(outputFileName);

        logger.log("about to run " + sb.toString());

        final Process start = Runtime.getRuntime().exec(sb.toString());
        final StreamConsumer error = new StreamConsumer(start.getErrorStream(), "ERROR", Optional.of(logger));
        final StreamConsumer info = new StreamConsumer(start.getInputStream(), "INFO", Optional.of(logger));
        error.start();
        info.start();

        while (start.isAlive()) {
            Thread.sleep(100);
        }

        logger.log("Finished transcode");
        return outputFileName;
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

    private void uploadToS3(final TranscodeMessage transcodeMessage, final String filename) {
        final PutObjectRequest putObjectRequest = new PutObjectRequest("live-streaming-shared-livestreamingdistribution-199wt2f91f0ik", "hls/" + transcodeMessage.getStreamId() + "/" + transcodeMessage.getProfile().getProfileName() + ".ts", new File(filename));
        this.amazonS3Client.putObject(putObjectRequest);
    }
}
