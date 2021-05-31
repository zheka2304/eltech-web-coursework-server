package com.eltech.web.server.socket;

import com.google.gson.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SocketHandler extends TextWebSocketHandler {
    public static final String EVENT_PEER_INIT = "peer_init";
    public static final String EVENT_SDP = "sdp";
    public static final String EVENT_CANDIDATE = "candidate";

    private static class PeerSession {
        public final WebSocketSession session;
        public String peerUid = null;

        PeerSession(WebSocketSession session) {
            this.session = session;
        }

        public void setPeerUid(String peerUid) {
            this.peerUid = peerUid;
        }

        public String getPeerUid() {
            return peerUid;
        }

        boolean isOpened() {
            return session.isOpen();
        }

        boolean isExpired() { return peerUid != null && !session.isOpen(); }
    }

    private final Object mapLock = new Object();
    private final Map<String, PeerSession> uninitializedSessions = new HashMap<>();
    private final Map<String, PeerSession> peerSessionBySessionId = new HashMap<>();
    private final Map<String, List<PeerSession>> peerSessionByUid = new HashMap<>();

    private static final int EXPIRED_SESSION_RELEASE_INTERVAL = 5000;

    private long lastExpiredSessionsRelease = System.currentTimeMillis();

    private int releaseExpiredSessions(Map<String, PeerSession> map) {
        int initialSize = map.size();
        map.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return initialSize - map.size();
    }

    private synchronized void releaseExpiredSessionsIfRequired() {
        if (lastExpiredSessionsRelease + EXPIRED_SESSION_RELEASE_INTERVAL < System.currentTimeMillis()) {
            lastExpiredSessionsRelease = System.currentTimeMillis();
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }

                int releasedCount = 0;
                synchronized (mapLock) {
                    releasedCount += releaseExpiredSessions(uninitializedSessions);
                    releasedCount += releaseExpiredSessions(peerSessionBySessionId);

                    peerSessionByUid.forEach((key, value) -> value.removeIf(PeerSession::isExpired));
                    peerSessionByUid.entrySet().removeIf(entry -> entry.getValue().isEmpty());
                }
                if (releasedCount > 0) {
                    System.out.println("released " + releasedCount + " expired socket sessions, " + (uninitializedSessions.size() + peerSessionBySessionId.size()) + " sessions remaining");
                }
            }).start();
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonObject json = JsonParser.parseString(message.getPayload()).getAsJsonObject();
            String type = json.get("type").getAsString();
            JsonElement payload = json.get("payload");
            handleEvent(session, message, type, payload);
        } catch (JsonParseException | IllegalStateException e) {
            // if data cannot be parsed, or type or payload cannot be acquired - ignore it
            e.printStackTrace();
        }

        releaseExpiredSessionsIfRequired();
    }

    private static class EventPeerInit {
        String uid;
    }

    private static class EventWithTargetPeer {
        String senderUid;
        String targetUid;
    }

    private final Gson gson = new GsonBuilder().create();

    private void handleEvent(WebSocketSession session, TextMessage message, String type, JsonElement payload) {
        if (EVENT_PEER_INIT.equals(type)) {
            try {
                EventPeerInit event = gson.fromJson(payload, EventPeerInit.class);
                synchronized (mapLock) {
                    PeerSession peer = uninitializedSessions.remove(session.getId());
                    if (peer != null) {
                        // System.out.println("initialized peer " + event.uid);
                        peer.setPeerUid(event.uid);
                        peerSessionBySessionId.put(session.getId(), peer);
                        peerSessionByUid.computeIfAbsent(peer.getPeerUid(), key -> new ArrayList<>()).add(peer);
                    }
                }
            } catch (JsonParseException e) {
                e.printStackTrace();
            }
        } else if (EVENT_SDP.equals(type) || EVENT_CANDIDATE.equals(type)) {
            try {
                EventWithTargetPeer event = gson.fromJson(payload, EventWithTargetPeer.class);

                synchronized (mapLock) {
                    // check if sender id is valid
                    List<PeerSession> possibleSenders = peerSessionByUid.get(event.senderUid);
                    if (possibleSenders == null || !possibleSenders.contains(peerSessionBySessionId.get(session.getId()))) {
                        return;
                    }

                    // forward event to target
                    List<PeerSession> targets = peerSessionByUid.get(event.targetUid);
                    if (targets != null) {for (PeerSession target : targets) {
                            try {
                                target.session.sendMessage(message);
                                // System.out.println("sending message from " + event.senderUid + " to " + event.targetUid + ", payload: " + message.getPayload());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (JsonParseException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        uninitializedSessions.put(session.getId(), new PeerSession(session));
        releaseExpiredSessionsIfRequired();
    }
}