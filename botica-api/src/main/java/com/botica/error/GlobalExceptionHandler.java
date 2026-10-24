package com.botica.error;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

// Todas las respuestas de error tienen la forma { "message": "..." } (lo que lee el frontend)
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record Err(String message) {}

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Err> api(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(new Err(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Err> validacion(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new Err(msg));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Err> cuerpo(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new Err("Cuerpo de la solicitud inválido"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Err> integridad(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new Err("No se pudo guardar: hay un valor duplicado (DNI, correo, lote…) o el registro está en uso."));
    }
}
