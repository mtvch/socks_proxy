package ru.nsu.fit.g19202.karpov.socks.exceptions;

import java.io.Serial;

public class SOCKSException extends Exception {
    @Serial
    private static final long serialVersionUID = -3868260968867472342L;

    public SOCKSException() {
        super("SOCKS exception");
    }
    public SOCKSException(String message) {
        super(message);
    }
}