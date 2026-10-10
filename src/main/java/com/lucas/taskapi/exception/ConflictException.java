package com.lucas.taskapi.exception;

/**
 * Lançada quando a operação conflita com o estado atual, por exemplo um e-mail já cadastrado.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
