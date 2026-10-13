package com.northstar.commerce.payment.api;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String,Object> missing(Exception ex){return error(ex);}
    @ExceptionHandler(IllegalStateException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String,Object> bad(Exception ex){return error(ex);}
    private Map<String,Object> error(Exception ex){return Map.of("message",ex.getMessage(),"timestamp", Instant.now().toString());}
}

