package com.gabriele.telegrambot.modes.youtube.messages;

public class DownloadCompleted {
    String jobId;
    String fileId;
    String url;

    public DownloadCompleted(String jobId, String fileId, String url) {
        this.jobId = jobId;
        this.fileId = fileId;
        this.url = url;
    }

    public String getJobId() {
        return jobId;
    }

    public String getFileId() {
        return fileId;
    }

    public String getUrl() {
        return url;
    }
}
