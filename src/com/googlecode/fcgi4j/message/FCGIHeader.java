package com.googlecode.fcgi4j.message;

import com.googlecode.fcgi4j.constant.FCGIHeaderType;

import java.nio.ByteBuffer;

/**
 * @author panzd
 */
public class FCGIHeader {

    public static final int FCGI_HEADER_LEN = 8;
    public static final int FCGI_VERSION_1 = 1;
    public static final int ID = 1;
    private int version;
    private FCGIHeaderType type;
    private int id;
    private int length;
    private int padding;

    public FCGIHeader(FCGIHeaderType type, int length) {
        this.version = FCGI_VERSION_1;
        this.type = type;
        this.id = ID;
        this.length = length;
    }

    private FCGIHeader() {
    }

    public static FCGIHeader parse(ByteBuffer buffer) {
        FCGIHeader header = new FCGIHeader();
        header.version = buffer.get();
        header.type = FCGIHeaderType.valueOf(buffer.get());
        header.id = buffer.getShort() & 0xffff;
        header.length = buffer.getShort() & 0xffff;
        header.padding = buffer.get();

        return header;
    }

    /**
     * @return the version
     */
    public int getVersion() {
        return version;
    }

    /**
     * @return the type
     */
    public FCGIHeaderType getType() {
        return type;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @return the length
     */
    public int getLength() {
        return length;
    }

    /**
     * @return the padding
     */
    public int getPadding() {
        return padding;
    }

    public ByteBuffer getByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(FCGI_HEADER_LEN);

        buffer.put((byte) getVersion());
        buffer.put((byte) getType().getId());
        buffer.putShort((short) (id & 0xffff));
        buffer.putShort((short) (length & 0xffff));
        buffer.put((byte) getPadding());

        buffer.rewind();
        return buffer;
    }
}
