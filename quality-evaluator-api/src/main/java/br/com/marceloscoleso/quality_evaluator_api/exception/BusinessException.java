package br.com.marceloscoleso.quality_evaluator_api.exception;

public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
