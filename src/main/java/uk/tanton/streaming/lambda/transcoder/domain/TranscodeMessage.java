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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TranscodeMessage that = (TranscodeMessage) o;

        if (!streamId.equals(that.streamId)) return false;
        if (!profile.equals(that.profile)) return false;
        if (!bucket.equals(that.bucket)) return false;
        return key.equals(that.key);

    }

    @Override
    public int hashCode() {
        int result = streamId.hashCode();
        result = 31 * result + profile.hashCode();
        result = 31 * result + bucket.hashCode();
        result = 31 * result + key.hashCode();
        return result;
    }
}
