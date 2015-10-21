/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.googlecode.fcgi4j.constant;

/**
 * @author panzd
 */
public enum FCGIProtocolStatus {
    FCGI_REQUEST_COMPLETE {
        @Override
        public int getId() {
            return 0;
        }
    },
    FCGI_CANT_MPX_CONN {
        @Override
        public int getId() {
            return 1;
        }
    },
    FCGI_OVERLOADED {
        @Override
        public int getId() {
            return 2;
        }
    },
    FCGI_UNKNOWN_ROLE {
        @Override
        public int getId() {
            return 3;
        }
    };
    private static final FCGIProtocolStatus[] statusMap;

    static {
        statusMap = new FCGIProtocolStatus[4];
        for (FCGIProtocolStatus status : values()) {
            statusMap[status.getId()] = status;
        }
    }

    public static FCGIProtocolStatus valueOf(int id) {
        return statusMap[id];
    }

    public abstract int getId();
}
