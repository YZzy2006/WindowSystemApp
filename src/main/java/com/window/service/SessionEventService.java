package com.window.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SessionEventService {

    private final Map<Integer, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void register(Integer adminId, SseEmitter emitter) {
        emitters.computeIfAbsent(adminId, k -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(adminId, emitter));
        emitter.onTimeout(() -> removeEmitter(adminId, emitter));
        emitter.onError(e -> removeEmitter(adminId, emitter));
    }

    public void sendKickEvent(Integer adminId, String message) {
        Set<SseEmitter> set = emitters.remove(adminId);
        if (set == null || set.isEmpty()) return;
        for (SseEmitter emitter : set) {
            try {
                emitter.send(SseEmitter.event().name("kick").data(message));
                // 不主动 complete()：send 和 complete 之间客户端可能断开，
                // 此时 complete() 会操作已失效的 AsyncContext 触发 Tomcat 警告。
                // 已从 map 移除，emitter 会通过超时回调自然清理。
            } catch (Exception e) {
                log.debug("发送踢出事件失败（客户端可能已断开）: {}", e.getMessage());
            }
        }
    }

    private void removeEmitter(Integer adminId, SseEmitter emitter) {
        Set<SseEmitter> set = emitters.get(adminId);
        if (set != null) {
            set.remove(emitter);
            if (set.isEmpty()) emitters.remove(adminId, set);
        }
    }
}
