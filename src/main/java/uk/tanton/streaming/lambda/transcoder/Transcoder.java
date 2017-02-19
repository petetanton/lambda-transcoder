package uk.tanton.streaming.lambda.transcoder;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import uk.tanton.streaming.lambda.transcoder.domain.Profile;

import java.io.IOException;
import java.util.Optional;

public class Transcoder {
    private final Optional<LambdaLogger> logger;
    private final String ffmpegLocation;

    public Transcoder(final String ffmpegLocation, final LambdaLogger logger) {
        this.ffmpegLocation = ffmpegLocation;
        this.logger = Optional.of(logger);
    }

    public Transcoder(String ffmpegLocation) {
        this.ffmpegLocation = ffmpegLocation;
        this.logger = Optional.empty();
    }

    public String transcode(final Profile profile, final String inputFilePath, final String outputFilePath) throws InterruptedException, IOException {
        log("ffmpeg is at: " + ffmpegLocation);
        final StringBuilder sb = new StringBuilder();
        sb.append(ffmpegLocation).append(" -i ").append(inputFilePath)
                .append(" -vcodec ").append(profile.getVideoCodec())
                .append(" -x264opts ").append(profile.getX264Opts())
                .append(" -b:v ").append(profile.getVideoBitrate());
        if (profile.getHeight() > 0 && profile.getWidth() > 0) {
            sb.append(" -vf scale=").append(profile.getWidth()).append(":").append(profile.getHeight());
        }
        sb.append(" -acodec copy")
                .append(" -threads 0 -preset superfast")
                .append(" ").append(outputFilePath);

        log("about to run " + sb.toString());

        final Process start = Runtime.getRuntime().exec(sb.toString());
        final StreamConsumer error = new StreamConsumer(start.getErrorStream(), "ERROR", logger);
        final StreamConsumer info = new StreamConsumer(start.getInputStream(), "INFO", logger);
        error.start();
        info.start();

        while (start.isAlive()) {
            Thread.sleep(100);
        }

        log("Finished transcode");
        return outputFilePath;
    }

    private void log(final String msg) {
        if (logger.isPresent()) {
            logger.get().log(msg);
        } else {
            System.out.println(msg);    // NOSONAR
        }
    }
}
