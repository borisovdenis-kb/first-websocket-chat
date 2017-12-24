package ru.intodayer.endpoints;

import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.PathParam;
import ru.intodayer.model.Message;
import javax.websocket.*;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.io.IOException;
import java.util.Set;


@ServerEndpoint(
    value = "/chat/{username}",
    decoders = MessageDecoder.class,
    encoders = MessageEncoder.class
)
public class TestEndpoint {
    private static Set<TestEndpoint> instances = new CopyOnWriteArraySet<>();
    private static HashMap<String, String> users = new HashMap<>();
    private Session session;

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username) throws IOException {
        this.session = session;
        instances.add(this);
        users.put(session.getId(), username);

        Message message = new Message();
        message.setFrom(username);
        message.setContent("User (" + username + ") Connected!");
        message.setConnectionId(session.getId());
        broadcast(message);
    }

    @OnMessage
    public void onMessage(Session session, Message message) throws IOException {
        message.setFrom(users.get(session.getId()));
        message.setConnectionId(session.getId());
        broadcast(message);
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        instances.remove(this);
        Message message = new Message();
        message.setFrom(users.get(session.getId()));
        message.setContent("Disconnected!");
        broadcast(message);
    }

    @OnError
    public void onError(Session session, Throwable throwable) throws IOException {
        // - handle error
    }

    private static void broadcast(Message message) {
        instances.forEach(endpoint -> {
            synchronized (endpoint) {
                try {
                    endpoint.session.getBasicRemote().sendObject(message);
                } catch (IOException | EncodeException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
