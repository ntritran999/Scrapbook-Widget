package com.group04.scrapbookwidget.ui.group;

import com.group04.scrapbookwidget.data.model.Message;
import java.util.List;
import java.util.Objects;

public class MessageWrapper {
    public final Message message;
    public final List<Message.SeenBy> lastSeenUsers;

    public MessageWrapper(Message message, List<Message.SeenBy> lastSeenUsers) {
        this.message = message;
        this.lastSeenUsers = lastSeenUsers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageWrapper that = (MessageWrapper) o;
        return Objects.equals(message, that.message) &&
               Objects.equals(lastSeenUsers, that.lastSeenUsers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, lastSeenUsers);
    }
}
