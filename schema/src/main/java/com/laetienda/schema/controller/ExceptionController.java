package com.laetienda.schema.controller;

import com.laetienda.lib.exception.NotValidCustomException;
import com.laetienda.lib.model.Mistake;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ExceptionController extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotValidCustomException.class)
    public ResponseEntity<Mistake> handleNotValidCustomException(NotValidCustomException ex){
        return new ResponseEntity<Mistake>(ex.getMistake(), ex.getStatus());
    }

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<String> handleHttpStatusCodeException(HttpStatusCodeException e){
        return new ResponseEntity<String>(e.getMessage(), e.getStatusCode());
    }
}
