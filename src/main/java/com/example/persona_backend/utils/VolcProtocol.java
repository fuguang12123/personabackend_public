package com.example.persona_backend.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * 火山引擎 WebSocket 协议消息封装类
 * (从 VolcEngineUtils 中提取，去除 Lombok 依赖以保证兼容性)
 */
public class VolcProtocol {

    // ================= 枚举定义 =================

    public enum MsgType {
        FULL_CLIENT_REQUEST((byte) 0b1),
        AUDIO_ONLY_SERVER((byte) 0b1011),
        ERROR((byte) 0b1111),
        UNKNOWN((byte) 0);

        private final byte value;

        MsgType(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        public static MsgType fromValue(int val) {
            for (MsgType t : values()) if (t.value == val) return t;
            return UNKNOWN;
        }
    }

    public enum MsgTypeFlagBits {
        NO_SEQ((byte) 0),
        POSITIVE_SEQ((byte) 0b1),
        NEGATIVE_SEQ((byte) 0b11),
        WITH_EVENT((byte) 0b100);

        private final byte value;

        MsgTypeFlagBits(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        public static MsgTypeFlagBits fromValue(int val) {
            for (MsgTypeFlagBits t : values()) if (t.value == val) return t;
            return NO_SEQ;
        }
    }

    public enum EventType {
        NONE(0),
        START_CONNECTION(1),
        FINISH_CONNECTION(2),
        CONNECTION_STARTED(50),
        CONNECTION_FINISHED(52),
        CONNECTION_FAILED(51),
        SESSION_FINISHED(152);

        private final int value;

        EventType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static EventType fromValue(int val) {
            for (EventType t : values()) if (t.value == val) return t;
            return NONE;
        }
    }

    // ================= 消息类主体 =================

    public static class Message {
        private byte version = 1;
        private byte headerSize = 1;
        private MsgType type;
        private MsgTypeFlagBits flag;
        private byte serialization = 1;
        private byte compression = 0;

        private EventType event;
        private String sessionId;
        private int errorCode;
        private byte[] payload;

        public Message(MsgType type, MsgTypeFlagBits flag) {
            this.type = type;
            this.flag = flag;
        }

        // 核心反序列化逻辑
        public static Message unmarshal(byte[] data) {
            ByteBuffer buffer = ByteBuffer.wrap(data);

            // 1. 读取 Type & Flag
            byte typeAndFlag = data[1];
            MsgType type = MsgType.fromValue((typeAndFlag >> 4) & 0x0F);
            MsgTypeFlagBits flag = MsgTypeFlagBits.fromValue(typeAndFlag & 0x0F);

            // 2. 读取 Version & HeaderSize
            int versionAndHeaderSize = buffer.get();
            int headerSizeVal = versionAndHeaderSize & 0x0F;

            // 3. 跳过 Byte 1 (Type/Flag) - 已经手动读了，buffer也需要跳过
            buffer.get();

            // 4. 读取 Serialization & Compression
            buffer.get();

            // 5. 跳过 Padding
            // HeaderSize4(1) -> 4 bytes. 4 - 3(already read) = 1 byte padding
            int headerSizeBytes = 4 * headerSizeVal;
            int paddingSize = headerSizeBytes - 3;
            while (paddingSize > 0) {
                buffer.get();
                paddingSize--;
            }

            Message message = new Message(type, flag);

            // 6. 读取 Sequence (若有)
            if (flag == MsgTypeFlagBits.POSITIVE_SEQ || flag == MsgTypeFlagBits.NEGATIVE_SEQ) {
                if (buffer.remaining() >= 4) buffer.getInt();
            }

            // 7. 读取 Event (若有)
            if (flag == MsgTypeFlagBits.WITH_EVENT) {
                if (buffer.remaining() >= 4) {
                    int eventVal = buffer.getInt();
                    message.setEvent(EventType.fromValue(eventVal));
                }

                // 读取 SessionID
                if (type != MsgType.ERROR && !isConnectionEvent(message.getEvent())) {
                    if (buffer.remaining() >= 4) {
                        int sessionIdLen = buffer.getInt();
                        if (sessionIdLen > 0 && buffer.remaining() >= sessionIdLen) {
                            byte[] sidBytes = new byte[sessionIdLen];
                            buffer.get(sidBytes);
                            message.setSessionId(new String(sidBytes, StandardCharsets.UTF_8));
                        }
                    }
                }
            }

            // 8. 读取 ErrorCode (若有)
            if (type == MsgType.ERROR) {
                if (buffer.remaining() >= 4) {
                    message.setErrorCode(buffer.getInt());
                }
            }

            // 9. 读取 Payload
            if (buffer.remaining() >= 4) {
                int payloadLen = buffer.getInt();
                if (payloadLen > 0 && buffer.remaining() >= payloadLen) {
                    byte[] p = new byte[payloadLen];
                    buffer.get(p);
                    message.setPayload(p);
                }
            }

            return message;
        }

        // 核心序列化逻辑
        public byte[] marshal() throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            buffer.write((version & 0x0F) << 4 | (headerSize & 0x0F));
            buffer.write((type.getValue() & 0x0F) << 4 | (flag.getValue() & 0x0F));
            buffer.write((serialization & 0x0F) << 4 | (compression & 0x0F));

            buffer.write(0x00); // Padding

            if (event != null) {
                buffer.write(intToBytes(event.getValue()));
            }

            if (payload != null && payload.length > 0) {
                buffer.write(intToBytes(payload.length));
                buffer.write(payload);
            } else if (type == MsgType.FULL_CLIENT_REQUEST && flag == MsgTypeFlagBits.NO_SEQ) {
                if (payload == null) buffer.write(intToBytes(0));
            }

            return buffer.toByteArray();
        }

        private static boolean isConnectionEvent(EventType evt) {
            return evt == EventType.START_CONNECTION || evt == EventType.FINISH_CONNECTION ||
                    evt == EventType.CONNECTION_STARTED || evt == EventType.CONNECTION_FINISHED ||
                    evt == EventType.CONNECTION_FAILED;
        }

        private byte[] intToBytes(int i) {
            return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(i).array();
        }

        // Getters/Setters
        public MsgType getType() { return type; }
        public EventType getEvent() { return event; }
        public byte[] getPayload() { return payload; }
        public int getErrorCode() { return errorCode; }
        public void setEvent(EventType event) { this.event = event; }
        public void setPayload(byte[] payload) { this.payload = payload; }
        public void setErrorCode(int errorCode) { this.errorCode = errorCode; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }
}