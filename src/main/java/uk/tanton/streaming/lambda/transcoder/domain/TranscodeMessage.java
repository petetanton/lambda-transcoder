package uk.tanton.streaming.lambda.transcoder.domain;

public class TranscodeMessage {

    private final String streamId;
    private final Profile profile;
    private final String bucket;
    private final String key;

    public TranscodeMessage(final String bucket, final String streamId, final Profile profile, final String key) {
        this.bucket = bucket;
        this.streamId = streamId;
        this.profile = profile;
        this.key = key;
    }

    public String getBucket() {
        return bucket;
    }

    public String getKey() {
        return key;
    }

    public Profile getProfile() {
        return profile;
    }

    public String getStreamId() {
        return streamId;
    }

    @Override
    public String toString() {
        return "TranscodeMessage{" +
                "bucket='" + bucket + '\'' +
                ", streamId='" + streamId + '\'' +
                ", profile=" + profile +
                ", key='" + key + '\'' +
                '}';
    }
}
