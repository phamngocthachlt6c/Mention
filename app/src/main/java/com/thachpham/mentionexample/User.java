package com.thachpham.mentionexample;

import org.thachpham.mention.MentionInput;

public class User extends MentionInput {

    private String name;

    public User(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getTitle() {
        return name;
    }
}
