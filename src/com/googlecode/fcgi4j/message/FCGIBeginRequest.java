package com.googlecode.fcgi4j.message;

import com.googlecode.fcgi4j.constant.FCGIHeaderType;
import com.googlecode.fcgi4j.constant.FCGIRole;

import java.nio.ByteBuffer;

/**
 * @author panzd
 */
public class FCGIBeginRequest {
    public static final int FCGI_BEGIN_REQUEST_LEN = 8;
    public static final int FCGI_KEEP_CONN = 1;
    private FCGIHeader header;
    private FCGIRole role;
    private boolean keepAlive;

    public FCGIBeginRequest(FCGIRole role, boolean keepAlive) {
        header = new FCGIHeader(FCGIHeaderType.FCGI_BEGIN_REQUEST, FCGI_BEGIN_REQUEST_LEN);

        this.role = role;
        this.keepAlive = keepAlive;
    }

    public ByteBuffer[] getByteBuffers() {
        ByteBuffer[] buffers = new ByteBuffer[2];
        buffers[0] = header.getByteBuffer();

        ByteBuffer buffer = ByteBuffer.allocate(FCGI_BEGIN_REQUEST_LEN);
        buffer.putShort((short) role.getId());
        buffer.put((byte) (keepAlive ? FCGI_KEEP_CONN : 0));
        buffer.rewind();

        buffers[1] = buffer;

        return buffers;
    }
}
