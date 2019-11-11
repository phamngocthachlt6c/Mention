package org.thachpham.mention;

class Mention {
    private String mentionText;
    private int startInMessage;
    private int endInMessage;

    int getStartInMessage() {
        return startInMessage;
    }

    void setStartInMessage(int startInMessage) {
        this.startInMessage = startInMessage;
    }

    int getEndInMessage() {
        return endInMessage;
    }

    void setEndInMessage(int endInMessage) {
        this.endInMessage = endInMessage;
    }

    String getMentionText() {
        return mentionText;
    }

    void setMentionText(String mentionText) {
        this.mentionText = mentionText;
    }
}
