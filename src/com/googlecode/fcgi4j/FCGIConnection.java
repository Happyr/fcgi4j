package com.googlecode.fcgi4j;

import com.googlecode.fcgi4j.constant.FCGIHeaderType;
import com.googlecode.fcgi4j.constant.FCGIRole;
import com.googlecode.fcgi4j.exceptions.FCGIException;
import com.googlecode.fcgi4j.exceptions.FCGIUnKnownHeaderException;
import com.googlecode.fcgi4j.message.*;
import com.googlecode.fcgi4j.util.IoUtils;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author panzd
 */
public class FCGIConnection implements GatheringByteChannel, ScatteringByteChannel, ByteChannel {

    private static final String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private String contentType = DEFAULT_CONTENT_TYPE;
    private static final String KEY_SCRIPT_FILENAME = "SCRIPT_FILENAME";
    private static final String KEY_QUERY_STRING = "QUERY_STRING";
    private static final String KEY_CONTENT_TYPE = "CONTENT_TYPE";
    private static final String KEY_CONTENT_LENGTH = "CONTENT_LENGTH";
    private static final String KEY_REQUEST_METHOD = "REQUEST_METHOD";
    private String scriptFilename;
    private String queryString;
    private int contentLength;
    private String requestMethod = "GET";
    private Map<String, FCGIParams> paramsMap;
    private Map<String, String> responseHeaders;
    private ByteBuffer headerBuffer;
    private ByteBuffer dataBuffer;
    private boolean open;
    private boolean paramsFlushed;
    private boolean stdinsWritten;
    private boolean stdinsFlushed;
    private FCGIEndRequest endRequest;
    private boolean readStarted;
    private boolean bufferEmpty;
    private SocketChannel socketChannel;

    private FCGIConnection() {
        paramsMap = new HashMap<String, FCGIParams>();
        responseHeaders = new HashMap<String, String>();
        headerBuffer = ByteBuffer.allocateDirect(FCGIHeader.FCGI_HEADER_LEN);
        dataBuffer = ByteBuffer.allocateDirect(64 * 1024);
    }

    public static FCGIConnection open() throws IOException {
        FCGIConnection connection = new FCGIConnection();
        connection.socketChannel = SocketChannel.open();
        connection.open = true;

        return connection;
    }

    /**
     * @return the queryString
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * @param queryString the queryString to set
     */
    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    /**
     * @return the contentType
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @param contentType the contentType to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @return the requestMethod
     */
    public String getRequestMethod() {
        return requestMethod;
    }

    /**
     * @param requestMethod the requestMethod to set
     */
    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    /**
     * @return the scriptFilename
     */
    public String getScriptFilename() {
        return scriptFilename;
    }

    /**
     * @return the contentLength
     */
    public int getContentLength() {
        return contentLength;
    }

    /**
     * @param contentLength the contentLength to set
     */
    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * @return the responseHeaders
     */
    public Map<String, String> getResponseHeaders() throws IOException {
        readyRead();
        return Collections.unmodifiableMap(responseHeaders);
    }

    /**
     * @return the endRequest
     */
    public boolean isRequestEnded() {
        return endRequest != null;
    }

    public FCGIEndRequest getEndRequest() {
        return endRequest;
    }

    public void connect(SocketAddress fcgiRemote) throws IOException {
        socketChannel.connect(fcgiRemote);
    }

    public void beginRequest(String scriptFilename, boolean keepAlive) throws IOException {
        beginRequest(scriptFilename, null, keepAlive);
    }

    public void beginRequest(String scriptFilename, String queryString, boolean keepAlive) throws IOException {
        socketChannel.write(new FCGIBeginRequest(FCGIRole.RESPONDER, keepAlive).getByteBuffers());

        this.scriptFilename = scriptFilename;
        this.queryString = queryString;

        endRequest = null;
        paramsFlushed = false;
        stdinsWritten = false;
        stdinsFlushed = false;
        readStarted = false;
        bufferEmpty = true;

        dataBuffer.clear();
        responseHeaders.clear();
        paramsMap.clear();

        setContentLength(0);
        setContentType(DEFAULT_CONTENT_TYPE);
        setRequestMethod("GET");
    }

    public void beginRequest(String scriptFilename) throws IOException {
        beginRequest(scriptFilename, true);
    }

    public void beginRequest(String scriptFilename, String queryString) throws IOException {
        beginRequest(scriptFilename, queryString, true);
    }

    public void addParams(String key, String value) {
        if (scriptFilename == null) {
            throw new FCGIException("please invoke method beginRequest() first");
        }

        if (paramsFlushed) {
            throw new FCGIException("params has flushed and lock");
        }

        paramsMap.put(key, new FCGIParams(key, value));
    }

    public void abortRequest() throws IOException {
        socketChannel.write(new FCGIHeader(FCGIHeaderType.FCGI_ABORT_REQUEST, 0).getByteBuffer());

        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        while (!isRequestEnded()) {
            read(byteBuffer);
            byteBuffer.clear();
        }
    }

    private void readResponseHeaders() {
        byte ch = dataBuffer.get();

        for (; ; ) {
            StringBuilder key = new StringBuilder();
            StringBuilder value = new StringBuilder();

            while (ch != ' ' && ch != '\r' && ch != '\n' && ch != ':') {
                key.append((char) ch);
                ch = dataBuffer.get();
            }

            while (ch == ' ' || ch == ':') {
                ch = dataBuffer.get();
            }

            while (ch != '\r' && ch != '\n') {
                value.append((char) ch);
                ch = dataBuffer.get();
            }

            responseHeaders.put(key.toString(), value.toString());

            int lnCount = 0;

            while ((ch == '\r' || ch == '\n') && lnCount < 3) {
                ch = dataBuffer.get();
                lnCount++;

            }

            if (lnCount == 3) {
                return;
            }
        }
    }

    private FCGIHeader readHeader() throws IOException {
        IoUtils.socketRread(socketChannel, headerBuffer);

        System.out.println(headerBuffer.toString());
        headerBuffer.flip();
        FCGIHeader header = FCGIHeader.parse(headerBuffer);
        headerBuffer.clear();
        return header;
    }

    private void flushParams() throws IOException {
        if (paramsFlushed) {
            return;
        }

        readyInternalParams();

        int buffersSize[] = new int[1];
        ArrayList<ByteBuffer[]> bufferArrays = new ArrayList<ByteBuffer[]>();

        for (FCGIParams params : paramsMap.values()) {
            collectBuffers(params.getByteBuffers(), bufferArrays, buffersSize);
        }

        collectBuffers(FCGIParams.NULL.getByteBuffers(), bufferArrays, buffersSize);

        ByteBuffer[] buffers = new ByteBuffer[buffersSize[0]];
        int pos = 0;
        for (ByteBuffer[] bufferArray : bufferArrays) {
            System.arraycopy(bufferArray, 0, buffers, pos, bufferArray.length);
            pos += bufferArray.length;
        }

        socketChannel.write(buffers);
        paramsFlushed = true;
    }

    private void flushStdins() throws IOException {
        if (!stdinsWritten || stdinsFlushed) {
            return;
        }

        socketChannel.write(FCGIStdin.NULL.getByteBuffers());
        stdinsFlushed = true;
    }

    private void readyInternalParams() {
        addParams(KEY_SCRIPT_FILENAME, getScriptFilename());

        String myQueryString = getQueryString();
        if (myQueryString != null) {
            addParams(KEY_QUERY_STRING, myQueryString);
        }

        String myRequestMethod = getRequestMethod().toUpperCase();

        addParams(KEY_REQUEST_METHOD, myRequestMethod);

        if (myRequestMethod.equals("POST")) {
            addParams(KEY_CONTENT_TYPE, getContentType());
            addParams(KEY_CONTENT_LENGTH, String.valueOf(getContentLength()));
        }
    }

    private void collectBuffers(ByteBuffer[] buffers, ArrayList<ByteBuffer[]> bufferArrays, int[] buffersSize) {
        bufferArrays.add(buffers);
        buffersSize[0] += buffers.length;
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        readyWrite();

        long written = 0;

        for (int i = offset; i < offset + length; i++) {
            ByteBuffer src = srcs[i];
            while (src.hasRemaining()) {
                IoUtils.safePut(dataBuffer, src);

                if (!dataBuffer.hasRemaining()) {
                    dataBuffer.flip();
                    written += writeStdin(createStdin(dataBuffer));
                    dataBuffer.clear();
                }
            }
        }

        if (dataBuffer.hasRemaining()) {
            dataBuffer.flip();
            written += writeStdin(createStdin(dataBuffer));
            dataBuffer.clear();
        }

        return written;
    }

    @Override
    public long write(ByteBuffer[] srcs) throws IOException {
        return write(srcs, 0, srcs.length);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        readyWrite();

        int written = 0;

        while (src.hasRemaining()) {
            IoUtils.safePut(dataBuffer, src);

            dataBuffer.flip();
            written += writeStdin(createStdin(dataBuffer));
            dataBuffer.clear();
        }

        return written;
    }

    private void readyWrite() throws IOException {
        if (scriptFilename == null) {
            throw new FCGIException("please invoke method beginRequest() first");
        }

        flushParams();

        if (stdinsFlushed) {
            throw new FCGIException("stdin has flushed and lock, can't be writen");
        }

        checkRequestFinished();

    }

    private FCGIStdin createStdin(ByteBuffer buffer) {
        if (buffer.limit() == 0) {
            return null;
        }

        byte[] data = new byte[buffer.limit()];
        buffer.get(data);

        return new FCGIStdin(data);
    }

    private long writeStdin(FCGIStdin stdin) throws IOException {
        if (stdin == null) {
            return 0;
        }

        ByteBuffer[] buffers = stdin.getByteBuffers();
        socketChannel.write(buffers);
        stdinsWritten = true;

        return stdin.getLength();
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() throws IOException {
        socketChannel.close();
        open = false;
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        readyRead();

        long read = 0;

        int available = 0, padding = 0;

        int maxLength = Math.min(offset + length, dsts.length);

        outer:
        for (int i = offset; i < maxLength; i++) {
            ByteBuffer dst = dsts[i];

            if (available == 0) {
                read += readBufferedData(dst);
            } else {
                read += readStdoutData(dst, available, padding);
            }

            while (dst.hasRemaining()) {
                FCGIHeader header = readHeader();

                if (header.getType() == FCGIHeaderType.FCGI_STDOUT && header.getLength() != 0) {
                    int currentRead = readStdoutData(dst, header.getLength(), header.getPadding());

                    available = header.getLength() - currentRead;
                    padding = header.getPadding();

                    read += currentRead;
                } else {
                    if (header.getType() == FCGIHeaderType.FCGI_END_REQUEST) {
                        finishRequest(header);
                    }

                    break outer;
                }
            }
        }

        if (available != 0) {
            bufferStdoutData(available, padding);
        }

        return read;
    }

    @Override
    public long read(ByteBuffer[] dsts) throws IOException {
        return read(dsts, 0, dsts.length);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        readyRead();

        int read = readBufferedData(dst);

        while (dst.hasRemaining()) {
            FCGIHeader header = readHeader();

            if (header.getType() == FCGIHeaderType.FCGI_STDOUT && header.getLength() != 0) {
                int currentRead = readStdoutData(dst, header.getLength(), header.getPadding());

                if (currentRead < header.getLength()) {
                    bufferStdoutData(header.getLength() - currentRead, header.getPadding());
                }

                read += currentRead;
            } else {
                if (header.getType() == FCGIHeaderType.FCGI_END_REQUEST) {
                    finishRequest(header);
                }

                break;
            }
        }

        return read;
    }

    private void readyRead() throws IOException {
        if (!readStarted) {
            flushParams();
            flushStdins();

            try {
                FCGIHeader firstHeader = readHeader();
                if (firstHeader.getType() == FCGIHeaderType.FCGI_STDOUT) {
                    bufferStdoutData(firstHeader.getLength(), firstHeader.getPadding());
                }

            } catch (FCGIUnKnownHeaderException e) {
                throw e;
            }

            readResponseHeaders();

            readStarted = true;
        }

        checkRequestFinished();
    }

    private void checkRequestFinished() {
        if (endRequest != null) {
            throw new FCGIException("the request has Finished");
        }
    }

    private int bufferStdoutData(int available, int padding) throws IOException {
        int read = readStdoutData(dataBuffer, available, padding);

        dataBuffer.flip();

        bufferEmpty = false;
        return read;
    }

    private int readBufferedData(ByteBuffer dst) {
        if (bufferEmpty) {
            return 0;
        }

        int read = IoUtils.safePut(dst, dataBuffer);

        if (!dataBuffer.hasRemaining()) {
            dataBuffer.clear();
            bufferEmpty = true;
        }

        return read;
    }

    private int readStdoutData(ByteBuffer buffer, int available, int padding) throws IOException {
        if (!buffer.hasRemaining()) {
            return 0;
        }

        int read;
        if (buffer.remaining() > available) {
            int limit = buffer.limit();

            buffer.limit(buffer.position() + available);
            read = IoUtils.socketRread(socketChannel, buffer);
            buffer.limit(limit);

            if (padding != 0) {
                ByteBuffer paddingBuffer = ByteBuffer.allocate(padding);
                IoUtils.socketRread(socketChannel, paddingBuffer);
            }
        } else {
            read = IoUtils.socketRread(socketChannel, buffer);
        }

        return read;
    }

    private void finishRequest(FCGIHeader header) throws IOException {
        ByteBuffer endBuffer = ByteBuffer.allocate(8);
        IoUtils.socketRread(socketChannel, endBuffer);
        endBuffer.rewind();
        endRequest = FCGIEndRequest.parse(header, endBuffer);
    }
}