package uk.tanton.streaming.lambda.transcoder;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.tanton.streaming.lambda.transcoder.domain.Profile;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        underTest = new Transcoder(FFMPEG);

        final Profile profile = new Profile();
        profile.setVideoCodec("libx264");
        profile.setVideoBitrate(1000000);
        profile.setHeight(1080);
        profile.setWidth(1920);
        profile.setX264Opts("keyint=50:min-keyint=25:scenecut=-1");

        try {
            final String actual = underTest.transcode(profile, "src/test/resources/input.ts", "src/test/resources/output.ts");
            assertEquals("src/test/resources/output.ts", actual);
            assertTrue(fileEquality(new File("src/test/resources/expected-output-1m.ts"), new File("src/test/resources/output.ts")));
        } catch (InterruptedException | IOException e) {
            fail("Got an exception");
            e.printStackTrace();
        }
    }

    private boolean fileEquality(final File expected, final File actual) throws IOException {
        return FileUtils.contentEquals(expected, actual);
    }
}