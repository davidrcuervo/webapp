package com.laetienda.lib.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laetienda.lib.model.Mistake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Date;

public class NotValidCustomException extends Exception{
    final private static Logger log = LoggerFactory.getLogger(NotValidCustomException.class);
    private Mistake mistake;
    private HttpStatusCode status;

    public NotValidCustomException(Exception e){
        super(e.getMessage());

        log.warn("EXCEPTION CACHED: {} -> {}", e.getClass().getSimpleName(), e.getMessage());
        switch (e) {
            case HttpClientErrorException ce -> {
                this.status = ce.getStatusCode();
                parseResponseBody(ce);
                break;
            }

            case HttpServerErrorException se -> {
                this.status = se.getStatusCode();
                parseResponseBody(se);
                break;
            }

            default -> {
                this.status = HttpStatus.INTERNAL_SERVER_ERROR;
                mistake = new Mistake(HttpStatus.INTERNAL_SERVER_ERROR.value());
                mistake.add("submit", e.getMessage());
            }
        }
    }

    public NotValidCustomException(String message, HttpStatus statuscode){
        super(message);
        this.status = statuscode;
        mistake = new Mistake(statuscode.value());
    }

    /**
     *
     * @param message error message
     * @param statuscode HttpStatus.code
     * @param key name of the pointer error. Used by forms
     */
    public NotValidCustomException(String message, HttpStatus statuscode, String key){
        super(message);
        this.status = statuscode;
        mistake = new Mistake(statuscode.value());
        addError(key, message);
    }

    public NotValidCustomException(String message, HttpStatusCode statuscode, String key){
        super(message);
        this.status = statuscode;
        mistake = new Mistake(statuscode.value());
        addError(key, message);
    }

    public NotValidCustomException(String message, HttpStatus statuscode, String pointer, String detail){
        super(message);
        this.status = statuscode;
        mistake = new Mistake(statuscode.value());
        addError(pointer, detail);
    }

    private void parseResponseBody(HttpStatusCodeException e){
        String body = e.getResponseBodyAsString();

        try {
            if(!body.isBlank()) {
                this.mistake = new ObjectMapper().readValue(body, Mistake.class);
            }
        }catch(JsonProcessingException je){
            log.error("Failed to parse response body from http error. {} -> {}", je.getClass().getSimpleName(), je.getMessage());

        }finally{
            if(this.mistake == null){
                String message = String.format("%s -> %s", e.getClass().getSimpleName(), e.getMessage());
                this.mistake = new Mistake(e.getStatusCode().value());
                this.mistake.add("submit", message);
            }
        }
    }

    public void addError(String key, String message){
        log.debug("Error: ${} -> {}", key, message);
        mistake.add(key, message);
    }

    public HttpStatusCodeException getHttpStatusCodeException(){
        if(status.is4xxClientError()){
            return new HttpClientErrorException(status, getMessage());

        } else if(status.is4xxClientError()){
            return new HttpServerErrorException(status, getMessage());
        } else{
            return new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, getMessage());
        }
    }

    public Mistake getMistake(){
        return mistake;
    }

    public void setMistake(Mistake mistake) {
        this.mistake = mistake;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }
}
