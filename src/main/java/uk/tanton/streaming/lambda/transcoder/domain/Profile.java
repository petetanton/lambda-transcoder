package uk.tanton.streaming.lambda.transcoder.domain;

public class Profile {

    private String profileName;
    private String videoCodec;
    private String x264Opts;
    private String audioCodec;
    private int videoBitrate;
    private int height;
    private int width;

    public String getAudioCodec() {
        return audioCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public int getVideoBitrate() {
        return videoBitrate;
    }

    public void setVideoBitrate(int videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    public String getX264Opts() {
        return x264Opts;
    }

    public void setX264Opts(String x264Opts) {
        this.x264Opts = x264Opts;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public String toString() {
        return "Profile{" +
                "audioCodec='" + audioCodec + '\'' +
                ", profileName='" + profileName + '\'' +
                ", videoCodec='" + videoCodec + '\'' +
                ", x264Opts='" + x264Opts + '\'' +
                ", videoBitrate=" + videoBitrate +
                ", height=" + height +
                ", width=" + width +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Profile profile = (Profile) o;

        if (videoBitrate != profile.videoBitrate) return false;
        if (height != profile.height) return false;
        if (width != profile.width) return false;
        if (!profileName.equals(profile.profileName)) return false;
        if (!videoCodec.equals(profile.videoCodec)) return false;
        if (!x264Opts.equals(profile.x264Opts)) return false;
        return audioCodec.equals(profile.audioCodec);

    }

    @Override
    public int hashCode() {
        int result = profileName.hashCode();
        result = 31 * result + videoCodec.hashCode();
        result = 31 * result + x264Opts.hashCode();
        result = 31 * result + audioCodec.hashCode();
        result = 31 * result + videoBitrate;
        result = 31 * result + height;
        result = 31 * result + width;
        return result;
    }
}
