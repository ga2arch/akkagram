package com.gabriele.telegrambot.modes.hackernews.internal;

public class HNStory {
    public long id;
    public String title;
    public String url;

    private String getCommentUrl() {
        return "https://news.ycombinator.com/item?id=" + id;
    }

    @Override
    public String toString() {
        return String.format("%s\n\n%s\n\n%s", title, url, getCommentUrl());
    }
}
