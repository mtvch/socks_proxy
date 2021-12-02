package ru.nsu.fit.g19202.karpov.socks.exceptions;

public class SOCKSException extends Exception {
    private static final long serialVersionUID = -3868260968867472342L;

    public SOCKSException() {
        super("SOCKS exception");
    }
    public SOCKSException(String message) {
        super(message);
    }
}