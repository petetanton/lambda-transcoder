package uk.tanton.streaming.lambda.transcoder;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.tanton.streaming.lambda.transcoder.domain.Profile;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class TranscoderTest {

    private static final String FFMPEG = "/usr/local/bin/ffmpeg";

    private Transcoder underTest;

    @Mock
    private LambdaLogger logger;


    @Before
    public void setup() {
        File oldFile = new File("src/test/resources/output.ts");
        if (oldFile.exists()) {
            oldFile.delete();
        }
    }

    @Test
    public void itShouldTranscodeVideo() {
        underTest = new Transcoder(FFMPEG, logger);

        final Profile profile = new Profile();
        profile.setVideoCodec("libx264");
        profile.setVideoBitrate(1000000);
        profile.setHeight(1080);
        profile.setWidth(1920);
        profile.setX264Opts("keyint=50:min-keyint=25:scenecut=-1");

        try {
            final String actual = underTest.transcode(profile, "src/test/resources/input.ts", "src/test/resources/output.ts");
            assertEquals("src/test/resources/output.ts", actual);
        } catch (InterruptedException | IOException e) {
            fail("Got an exception");
            e.printStackTrace();
        }


        final StringBuilder sb = new StringBuilder();
        sb.append(FFMPEG).append(" -i ").append("src/test/resources/input.ts")
                .append(" -vcodec ").append(profile.getVideoCodec())
                .append(" -x264opts ").append(profile.getX264Opts())
                .append(" -b:v ").append(profile.getVideoBitrate());
        if (profile.getHeight() > 0 && profile.getWidth() > 0) {
            sb.append(" -vf scale=").append(profile.getWidth()).append(":").append(profile.getHeight());
        }
        sb.append(" -acodec copy")
                .append(" -threads 0 -preset superfast")
                .append(" ").append("src/test/resources/output.ts");


        verify(logger).log("about to run " + sb.toString());
        verify(logger).log("ffmpeg is at: " + FFMPEG);
        verify(logger, times(64)).log(anyString());
        verifyNoMoreInteractions(logger);
    }
}