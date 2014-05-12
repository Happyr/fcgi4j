package com.googlecode.fcgi4j.message;

import com.googlecode.fcgi4j.FCGIException;
import com.googlecode.fcgi4j.constant.FCGIHeaderType;

import java.nio.ByteBuffer;

/**
 * @author panzd
 */
public class FCGIParams {
    public static final FCGIParams NULL = new FCGIParams();
    private FCGIHeader header;
    private String key;
    private String value;

    public FCGIParams(String key, String value) {
        if (key == null || value == null) {
            throw new FCGIException("FCGI_RARAMS's key and value can't be null");
        }

        int length = countLength(key) + countLength(value);
        header = new FCGIHeader(FCGIHeaderType.FCGI_PARAMS, length);

        this.key = key;
        this.value = value;
    }

    private FCGIParams() {
        header = new FCGIHeader(FCGIHeaderType.FCGI_PARAMS, 0);
    }

    public ByteBuffer[] getByteBuffers() {
        if (key == null || value == null) {
            return new ByteBuffer[]{header.getByteBuffer()};
        }

        ByteBuffer[] buffers = new ByteBuffer[2];
        buffers[0] = header.getByteBuffer();

        ByteBuffer byteBuffer = ByteBuffer.allocate(header.getLength());

        bufferLength(byteBuffer, key);
        bufferLength(byteBuffer, value);
        byteBuffer.put(key.getBytes());
        byteBuffer.put(value.getBytes());
        byteBuffer.rewind();

        buffers[1] = byteBuffer;

        return buffers;
    }

    private int countLength(String str) {
        int length = str.length();
        if (length < 0x80) {
            length += 1;
        } else {
            length += 4;
        }

        return length;
    }

    private void bufferLength(ByteBuffer byteBuffer, String str) {
        int length = str.length();

        if (length < 0x80) {
            byteBuffer.put((byte) length);
        } else {
            byteBuffer.putInt(length);
        }
    }
}
