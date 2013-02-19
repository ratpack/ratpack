package org.ratpackframework.session;

public interface SessionListener {

    void sessionInitiated(String id);

    void sessionTerminated(String id);

}
