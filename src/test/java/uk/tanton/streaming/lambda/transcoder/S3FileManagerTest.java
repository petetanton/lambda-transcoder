package uk.tanton.streaming.lambda.transcoder;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.tanton.streaming.lambda.transcoder.domain.Profile;
import uk.tanton.streaming.lambda.transcoder.domain.TranscodeMessage;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class S3FileManagerTest {
    private static final String BUCKET = "some-bucket-name";
    private static final String KEY = "some/s3/key.ts";
    private static final String BUCKET_DISTRO =  "live-streaming-shared-livestreamingdistribution-199wt2f91f0ik";

    private S3FileManager underTest;

    @Mock
    private AmazonS3Client amazonS3Client;
    @Mock
    private LambdaLogger logger;
    @Mock
    private S3ObjectInputStream mockedInputStream;
    @Mock
    private S3Object mockedS3Object;

    @Before
    public void setup() {
        final File file = new File("/tmp/key.ts");
        if (file.exists()) {
            file.delete();
        }
        this.underTest = new S3FileManager(amazonS3Client);
    }

    private void itAssetsNoMore() {
        verifyNoMoreInteractions(amazonS3Client, logger, mockedInputStream, mockedS3Object);
    }

    @Test
    public void itDownloadsAnObjectFromS3() throws IOException, InterruptedException {
        when(amazonS3Client.getObject(BUCKET, KEY)).thenReturn(mockedS3Object);
        when(mockedS3Object.getObjectContent()).thenReturn(mockedInputStream);
        when(mockedS3Object.getBucketName()).thenReturn(BUCKET);
        when(mockedS3Object.getKey()).thenReturn(KEY);
        when(mockedInputStream.read(new byte[1024])).thenReturn(1).thenReturn(1).thenReturn(-1);


        this.underTest.downloadObject(BUCKET, KEY, logger);

        verify(amazonS3Client).getObject(BUCKET, KEY);
        verify(logger).log("Writing s3 object to file");
        verify(mockedS3Object).getObjectContent();
        verify(mockedInputStream, times(3)).read(new byte[1024]);
        verify(mockedInputStream).close();
        verify(mockedS3Object, times(2)).getKey();
        itAssetsNoMore();

        final File file = new File("/tmp/key.ts");
        assertTrue(file.exists());
    }

    @Test
    public void itUploadsFilesToS3() {
        final String streamId = "streamId";
        final TranscodeMessage transcodeMessage = new TranscodeMessage(BUCKET, streamId, new Profile(), KEY);
        final ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        this.underTest.uploadToS3(transcodeMessage, "/tmp/key.ts");
        verify(amazonS3Client).putObject(putObjectRequestArgumentCaptor.capture());

        final PutObjectRequest actual = putObjectRequestArgumentCaptor.getValue();
        assertEquals(BUCKET_DISTRO, actual.getBucketName());
        assertEquals("hls/" + streamId + "/key.ts", actual.getKey());

        itAssetsNoMore();
    }

}