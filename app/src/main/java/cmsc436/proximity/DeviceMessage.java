package cmsc436.proximity;

import com.google.android.gms.nearby.messages.Message;


public class DeviceMessage {

    private final String mSender;
    private final Message mMessage;

    /**
     * Builds a new {@link Message} object along with the sender's name.
     */
    private DeviceMessage(String sender, String message) {
        this.mSender = sender;
        this.mMessage = new Message(message.getBytes());
    }

    protected Message getMessageBody() {
        return mMessage;
    }

    protected String getSender() {
        return mSender;
    }
}