package com.googlecode.fcgi4j.exceptions;

/**
 * com.googlecode.fcgi4j.exceptions
 *
 * @autor Tobias Nyholm
 */
public class FCGIInvalidHeaderException extends FCGIException {

    public FCGIInvalidHeaderException() {

        super("The HTTP header is invalid.");
    }
}
