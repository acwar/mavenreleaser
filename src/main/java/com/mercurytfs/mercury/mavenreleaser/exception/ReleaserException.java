package com.mercurytfs.mercury.mavenreleaser.exception;

public class ReleaserException extends Exception {

    public ReleaserException(String message, Exception exception){
        super(message, exception);
    }

    public ReleaserException(Exception exception){
        super(null, exception);
    }

    public ReleaserException(String message){
        super(message, null);
    }
}
