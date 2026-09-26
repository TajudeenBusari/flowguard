package com.flowguard.identity_service.exception;

import com.tjtechy.system.Result;
import com.tjtechy.system.StatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandlerAdvice {

  @ExceptionHandler(OrganizationAlreadyExistException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public Mono<Result> handleOrganizationAlreadyExist(OrganizationAlreadyExistException ex){
    return Mono.just(new Result(ex.getMessage(), false, StatusCode.CONFLICT));
  }

  @ExceptionHandler(WebExchangeBindException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Mono<Result> handleValidationException(WebExchangeBindException ex){
    String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getDefaultMessage())
            .findFirst()
            .orElse("Invalid request");
    return Mono.just(new Result(message, false, StatusCode.BAD_REQUEST));
  }

  @ExceptionHandler(OrganizationNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Mono<Result> handleOrganizationNotFoundException(OrganizationNotFoundException ex) {
    return Mono.just(new Result(ex.getMessage(), false, StatusCode.NOT_FOUND));
  }

  @ExceptionHandler(UserNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Mono<Result> handleUserNotFoundException(UserNotFoundException ex) {
    return Mono.just(new Result(ex.getMessage(), false, StatusCode.NOT_FOUND));
  }


  @ExceptionHandler(UserAlreadyExistsException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public Mono<Result> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
    return Mono.just(new Result(ex.getMessage(), false, StatusCode.CONFLICT));
  }

  @ExceptionHandler(BadCredentialsException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public Mono<Result> handleBadCredentialsException(BadCredentialsException ex) {
    return Mono.just(new Result("Invalid email or password", false, StatusCode.UNAUTHORIZED));
  }

  @ExceptionHandler(InvalidRoleOperationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Mono<Result> handleInvalidRoleOperationException(InvalidRoleOperationException ex) {
    return Mono.just(new Result(ex.getMessage(), false, StatusCode.BAD_REQUEST));
  }
}
