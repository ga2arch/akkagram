package com.gabriele.telegrambot.modes.hackernews.messages;

public class SetThresholdMessage {

    long userid;
    int threshold;

    public SetThresholdMessage(long userid, int threshold) {
        this.userid = userid;
        this.threshold = threshold;
    }

    public String getUserid() {
        return String.valueOf(userid);
    }

    public String getThreshold() {
        return String.valueOf(threshold);
    }
}
