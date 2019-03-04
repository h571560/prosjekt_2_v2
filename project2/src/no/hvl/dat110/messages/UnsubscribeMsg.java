package no.hvl.dat110.messages;

public class UnsubscribeMsg extends Message {

    // TODO:
    // Implement objectvariables, constructor, get/set-methods, and toString method
    private String unSubTo;

    public UnsubscribeMsg(String user, String unSubTo) {
        super(MessageType.SUBSCRIBE, user);
        this.unSubTo = unSubTo;
    }

    public String getUnSubTo() {

        return unSubTo;
    }

    public void setTopic(String topic) {

        this.unSubTo = unSubTo;
    }


    @Override
    public String toString() {
        return String.format("%s unsubscribe to: %s", super.toString(), this.unSubTo);
    }
}
