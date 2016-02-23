package com.gabriele.telegrambot.modes.youtube.messages;

public class DownloadCompleted {
    String jobId;

    public DownloadCompleted(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

}
