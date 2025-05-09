package org.example.jaipark_back.exception;

public class PostException extends RuntimeException {
    public PostException(String message) {
        super(message);
    }

    public static class PostNotFoundException extends PostException {
        public PostNotFoundException() {
            super("게시물을 찾을 수 없습니다.");
        }
    }

    public static class UnauthorizedException extends PostException {
        public UnauthorizedException() {
            super("해당 게시물에 대한 권한이 없습니다.");
        }
    }

    public static class InvalidInputException extends PostException {
        public InvalidInputException(String message) {
            super(message);
        }
    }
} 