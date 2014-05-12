/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.googlecode.fcgi4j.message;

import com.googlecode.fcgi4j.constant.FCGIProtocolStatus;

import java.nio.ByteBuffer;

/**
 * @author panzd
 */
public class FCGIEndRequest {
    private FCGIHeader header;
    private long appStatus;
    private FCGIProtocolStatus protocolStatus;


    private FCGIEndRequest() {
    }

    public static FCGIEndRequest parse(FCGIHeader header, ByteBuffer buffer) {
        FCGIEndRequest endRequest = new FCGIEndRequest();
        endRequest.header = header;
        endRequest.appStatus = buffer.getInt() & 0xffffffff;
        endRequest.protocolStatus = FCGIProtocolStatus.valueOf(buffer.get());

        return endRequest;
    }

    /**
     * @return the header
     */
    public FCGIHeader getHeader() {
        return header;
    }

    /**
     * @return the appStatus
     */
    public long getAppStatus() {
        return appStatus;
    }

    /**
     * @return the protocolStatus
     */
    public FCGIProtocolStatus getProtocolStatus() {
        return protocolStatus;
    }
}
