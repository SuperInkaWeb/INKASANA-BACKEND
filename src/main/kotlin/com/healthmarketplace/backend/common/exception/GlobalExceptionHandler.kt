package com.healthmarketplace.backend.common.exception

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        ex: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {

        val error = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "BUSINESS_ERROR",
            message = ex.message ?: "Business error",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {

        val error = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "BAD_REQUEST",
            message = ex.message ?: "Solicitud inválida",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(
        ex: IllegalStateException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {

        val error = ApiError(
            status = HttpStatus.CONFLICT.value(),
            error = "CONFLICT",
            message = ex.message ?: "Estado inválido de la operación",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error)
    }

    @ExceptionHandler(
        AuthorizationDeniedException::class,
        AccessDeniedException::class
    )
    fun handleAccessDeniedException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {

        val error = ApiError(
            status = HttpStatus.FORBIDDEN.value(),
            error = "FORBIDDEN",
            message = "No tienes permisos para acceder a este recurso",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {

        val firstError = ex.bindingResult.fieldErrors.firstOrNull()
        val message = firstError?.defaultMessage ?: "Validation error"

        val error = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "VALIDATION_ERROR",
            message = message,
            path = request.requestURI
        )

        return ResponseEntity.badRequest().body(error)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {

        val error = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "CONSTRAINT_VIOLATION",
            message = ex.message ?: "Constraint violation",
            path = request.requestURI
        )

        return ResponseEntity.badRequest().body(error)
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceeded(
        ex: MaxUploadSizeExceededException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {

        val error = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "FILE_TOO_LARGE",
            message = "La imagen no puede superar los 5 MB",
            path = request.requestURI
        )

        return ResponseEntity.badRequest().body(error)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {

        ex.printStackTrace()

        val error = ApiError(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "INTERNAL_SERVER_ERROR",
            message = "Unexpected internal server error",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}