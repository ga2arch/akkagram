package com.gabriele.telegrambot.modes.youtube.internals;

public class DownloadError {
    String jobId;
    String url;
    String error;

    public DownloadError(String jobId, String url, String error) {
        this.jobId = jobId;
        this.url = url;
        this.error = error;
    }

    public String getJobId() {
        return jobId;
    }

    public String getUrl() {
        return url;
    }

    public String getError() {
        return error;
    }
}
