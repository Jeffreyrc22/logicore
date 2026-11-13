package com.logicore.shared;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiError {

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<?> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode()).body(
      java.util.Map.of(
        "message",
        e.getReason() == null ? "Request rejected" : e.getReason()
      )
    );
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    IllegalArgumentException.class,
  })
  ResponseEntity<?> validation(Exception e) {
    return ResponseEntity.badRequest().body(
      java.util.Map.of(
        "message",
        "Check the required fields and allowed values."
      )
    );
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    org.springframework.orm.ObjectOptimisticLockingFailureException.class,
  })
  ResponseEntity<?> conflict(Exception e) {
    return ResponseEntity.status(409).body(
      java.util.Map.of(
        "message",
        "This record already exists, is referenced, or was changed. Refresh and retry."
      )
    );
  }

  @ExceptionHandler(
    org.springframework.web.multipart.MaxUploadSizeExceededException.class
  )
  ResponseEntity<?> size(Exception e) {
    return ResponseEntity.status(413).body(
      java.util.Map.of("message", "Maximum upload size is 10 MB.")
    );
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> unknown(Exception e) {
    return ResponseEntity.status(500).body(
      java.util.Map.of("message", "The request could not be completed.")
    );
  }
}
