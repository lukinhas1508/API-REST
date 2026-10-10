package com.lucas.taskapi.exception;

/**
 * Lançada quando uma operação é válida no formato, mas viola uma regra de negócio.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
