package engine;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
class QuizNotFoundException extends RuntimeException {
    public QuizNotFoundException(String message) {
        super(message);
    }
}
@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidQuizException extends RuntimeException {
    public InvalidQuizException(String message) {
        super(message);
    }
}
@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidUserDataException extends RuntimeException {
    public InvalidUserDataException(String msg) {
        super(msg);
    }
}
@ResponseStatus(HttpStatus.FORBIDDEN)
class AccessForbiddenException extends RuntimeException {
    public AccessForbiddenException(String msg) {
        super(msg);
    }
}