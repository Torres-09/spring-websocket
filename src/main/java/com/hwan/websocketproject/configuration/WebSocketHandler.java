package com.hwan.websocketproject.configuration;

import com.hwan.websocketproject.model.Message;
import com.hwan.websocketproject.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        log.info("sessionId: " + sessionId);
        sessions.put(sessionId, session);

        Message message = Message.builder()
                .sender(sessionId)
                .receiver("all")
                .build();
        message.newConnect();

        sessions.values().forEach(s ->{
            try {
                if(!s.getId().equals(sessionId)){
                    s.sendMessage(new TextMessage(Utils.getString(message)));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Message msg = Utils.getObject(message.getPayload());
        msg.setSender(session.getId());

        WebSocketSession receiver = sessions.get(msg.getReceiver());

        if (receiver != null && receiver.isOpen()) {
            receiver.sendMessage(new TextMessage(Utils.getString(msg)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();

        sessions.remove(sessionId);

        final Message message = new Message();
        message.closeConnect();
        message.setSender(sessionId);

        sessions.values().forEach(s->{
            try {
                s.sendMessage(new TextMessage(Utils.getString(message)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
