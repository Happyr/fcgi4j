package com.googlecode.fcgi4j.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author panzd
 */
public class IoUtils {

    public static int safePut(ByteBuffer dst, ByteBuffer src) {
        int r = dst.remaining();
        int l = src.limit();

        if (src.remaining() > r) {
            src.limit(src.position() + r);
        }

        dst.put(src);
        src.limit(l);

        return r;
    }

    public static int socketRread(SocketChannel channel, ByteBuffer buffer) throws IOException {
        int total = 0;
        while (buffer.hasRemaining()) {
            int read = channel.read(buffer);
            if (read != -1) {
                total += read;
            } else {
                break;
            }
        }

        return total;
    }
}
