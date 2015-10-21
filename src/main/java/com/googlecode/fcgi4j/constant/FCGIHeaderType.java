package com.googlecode.fcgi4j.constant;

import com.googlecode.fcgi4j.exceptions.FCGIUnKnownHeaderException;

/**
 * @author panzd
 */
public enum FCGIHeaderType {
    FCGI_BEGIN_REQUEST {
        @Override
        public int getId() {
            return 1;
        }
    },
    FCGI_ABORT_REQUEST {
        @Override
        public int getId() {
            return 2;
        }
    },
    FCGI_END_REQUEST {
        @Override
        public int getId() {
            return 3;
        }
    },
    FCGI_PARAMS {
        @Override
        public int getId() {
            return 4;
        }
    },
    FCGI_STDIN {
        @Override
        public int getId() {
            return 5;
        }
    },
    FCGI_STDOUT {
        @Override
        public int getId() {
            return 6;
        }
    },
    FCGI_STDERR {
        @Override
        public int getId() {
            return 7;
        }
    },
    FCGI_DATA {
        @Override
        public int getId() {
            return 8;
        }
    },
    FCGI_GET_VALUES {
        @Override
        public int getId() {
            return 9;
        }
    },
    FCGI_GET_VALUES_RESULT {
        @Override
        public int getId() {
            return 10;
        }
    },
    FCGI_UNKNOWN_TYPE {
        @Override
        public int getId() {
            return 11;
        }
    };
    private static final FCGIHeaderType[] typeMap;

    static {
        typeMap = new FCGIHeaderType[12];
        for (FCGIHeaderType type : values()) {
            typeMap[type.getId()] = type;
        }
    }

    public static FCGIHeaderType valueOf(int id) {
        try {
            return typeMap[id];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new FCGIUnKnownHeaderException("Header: " + id);
        }
    }

    public abstract int getId();
}
