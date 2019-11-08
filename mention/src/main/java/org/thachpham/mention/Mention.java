package org.thachpham.mention;

class Mention {
    private String mentionText;
    private int startInMessage;
    private int endInMessage;

    public int getStartInMessage() {
        return startInMessage;
    }

    public void setStartInMessage(int startInMessage) {
        this.startInMessage = startInMessage;
    }

    public int getEndInMessage() {
        return endInMessage;
    }

    public void setEndInMessage(int endInMessage) {
        this.endInMessage = endInMessage;
    }

    public String getMentionText() {
        return mentionText;
    }

    public void setMentionText(String mentionText) {
        this.mentionText = mentionText;
    }
}
