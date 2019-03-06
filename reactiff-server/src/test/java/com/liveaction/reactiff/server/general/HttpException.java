package com.liveaction.reactiff.server.general;

public final class HttpException extends RuntimeException {

    public final int status;

    public HttpException(int status, String message) {
        super(message);
        this.status = status;
    }

}
