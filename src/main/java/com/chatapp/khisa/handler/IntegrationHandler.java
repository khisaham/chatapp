package com.chatapp.khisa.handler;

import com.chatapp.khisa.model.Message;
import com.chatapp.khisa.model.MessageRecevided;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class IntegrationHandler extends TextWebSocketHandler {
    private Log logger = LogFactory.getLog(getClass());

    @Autowired
    @Qualifier("ids")
    public List<String> ids;

    private static ArrayList<WebSocketSession> sessions = new ArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        String id = UUID.randomUUID().toString().replace("-","").substring(0, 5);
        logger.info("HANDLE AFTER CONNECTION " + id);
        session.getAttributes().put("sessionID", id);
        ids.add(id);


        Message message = new Message();

        message.setType("SESSION_ID");
        message.setBody(id);

        session.sendMessage(new TextMessage(message.toString()));
        sessions.add(session);


        sessions
                .stream().filter(webSocketSession -> !webSocketSession.getAttributes().get("sessionID").toString().equalsIgnoreCase(id))
                .forEach(webSocketSession -> {
            Message messageNewUser = new Message();
            messageNewUser.setBody(id);
            messageNewUser.setType("NEW_USER");
            try {
                webSocketSession.sendMessage(new TextMessage(messageNewUser.toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions = (ArrayList<WebSocketSession>) sessions.stream()
                            .filter(webSocketSession -> !webSocketSession.getAttributes().get("sessionID").toString().equalsIgnoreCase(session.getAttributes().get("sessionID").toString()))
                            .collect(Collectors.toList());
        sessions
                .stream().filter(webSocketSession -> !webSocketSession.getAttributes().get("sessionID").toString().equalsIgnoreCase(session.getAttributes().get("sessionID").toString()))
                .forEach(webSocketSession -> {
                    Message messageNewUser = new Message();
                    messageNewUser.setBody(session.getAttributes().get("sessionID").toString());
                    messageNewUser.setType("CLOSE_USER");
                    try {
                        webSocketSession.sendMessage(new TextMessage(messageNewUser.toString()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        logger.info("HANDLE MESSAGE");
        logger.info("MESSAGE= " + message.toString());

        ObjectMapper objectMapper = new ObjectMapper();
        MessageRecevided messageRecevided = objectMapper.readValue(message.getPayload().toString(), MessageRecevided.class);

        String idSend = messageRecevided.getId();
        messageRecevided.setId(session.getAttributes().get("sessionID").toString());

        Message messageTo = new Message();

        messageTo.setType("NEW_MESSAGE");
        messageTo.setBody(messageRecevided);

        sendMessageToUser(idSend, new TextMessage(messageTo.toString()));
    }

    public void sendMessageToUser(String sessionID, TextMessage message) {
        for (WebSocketSession session: sessions) {
            if (session.getAttributes().get("sessionID").toString().equalsIgnoreCase(sessionID)) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
}
