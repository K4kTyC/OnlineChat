package chat.server;

public class Message {

    private final String text;
    private final String author;
    private volatile boolean isSentToAuthor = false;
    private volatile boolean isSentToAddressee = false;
    private volatile boolean isSavedToFile = false;

    public Message(String author, String text) {
        this.author = author;
        this.text = author + ": " + text;
    }

    public String getText() {
        return text;
    }

    public String getAuthor() {
        return author;
    }

    public boolean isSentToAuthor() {
        return isSentToAuthor;
    }

    public void setSentToAuthor(boolean sentToAuthor) {
        isSentToAuthor = sentToAuthor;
    }

    public boolean isSentToAddressee() {
        return isSentToAddressee;
    }

    public void setSentToAddressee(boolean sentToAddressee) {
        isSentToAddressee = sentToAddressee;
    }

    public boolean isSavedToFile() {
        return isSavedToFile;
    }

    public void setSavedToFile(boolean savedToFile) {
        isSavedToFile = savedToFile;
    }
}
