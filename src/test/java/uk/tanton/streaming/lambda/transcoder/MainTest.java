package uk.tanton.streaming.lambda.transcoder;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.tanton.streaming.lambda.transcoder.domain.TranscodeMessage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MainTest {


    private final ArrayList<SNSEvent.SNSRecord> records = new ArrayList<>();
    private String transcodeMessageString;
    private Main underTest;
    @Mock
    private SNSEvent snsEvent;
    @Mock
    private Context context;
    @Mock
    private SNSEvent.SNSRecord record;
    @Mock
    private LambdaLogger logger;
    @Mock
    private S3FileManager s3FileManager;

    @Before
    public void setup() throws IOException {
        transcodeMessageString = FileUtils.readFileToString(new File("src/test/resources/example-transcode-msg.json"), StandardCharsets.UTF_8);
        records.add(record);

        when(snsEvent.getRecords()).thenReturn(records);
        final SNSEvent.SNS t = new SNSEvent.SNS();
        t.setMessage(transcodeMessageString);
        when(record.getSNS()).thenReturn(t);
        when(context.getLogger()).thenReturn(logger);
        FileUtils.copyFile(new File("/usr/local/bin/ffmpeg"), new File("/tmp/ffmpeg"));
    }

    @Test
    public void itIsHappy() throws IOException, InterruptedException {
        underTest = new Main(s3FileManager);

        final ArgumentCaptor<TranscodeMessage> transcodeMessageArgumentCaptor = ArgumentCaptor.forClass(TranscodeMessage.class);
        final ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        underTest.handleRequest(snsEvent, context);

        verify(snsEvent).getRecords();
        verify(context, times(5)).getLogger();
        verify(record).getSNS();
        verify(logger, times(19)).log(anyString());
        verify(s3FileManager).downloadObject("bucket", "key", logger);
        verify(s3FileManager).uploadToS3(transcodeMessageArgumentCaptor.capture(), filenameArgumentCaptor.capture());
        verifyNoMoreInteractions(snsEvent, context, record, logger, s3FileManager);

        final TranscodeMessage actualTrancodeMessage = transcodeMessageArgumentCaptor.getValue();
        final String actualFilename = filenameArgumentCaptor.getValue();

        assertEquals(new Gson().fromJson(transcodeMessageString, TranscodeMessage.class), actualTrancodeMessage);
        assertEquals("/tmp/streamId-profileName-key", actualFilename);
    }

    @Test
    public void itThrowsATranscodeExceptionIfTheDownloadFromS3Fails() throws IOException, InterruptedException {
        final String message = "exception message";
        doThrow(new IOException(message)).when(s3FileManager).downloadObject("bucket", "key", logger);

        underTest = new Main(s3FileManager);

        try {
            underTest.handleRequest(snsEvent, context);
            fail("expected an exception");
        } catch (RuntimeException e) {
            assertTrue(e instanceof TranscodeException);
            assertEquals("error when downloading object from S3", e.getMessage());
            assertTrue(e.getCause() instanceof IOException);
            assertEquals(message, e.getCause().getMessage());
        }


        verify(snsEvent).getRecords();
        verify(context, times(2)).getLogger();
        verify(record).getSNS();
        verify(logger).log(anyString());
        verify(s3FileManager).downloadObject("bucket", "key", logger);

        verifyNoMoreInteractions(snsEvent, context, record, logger, s3FileManager);

    }


}