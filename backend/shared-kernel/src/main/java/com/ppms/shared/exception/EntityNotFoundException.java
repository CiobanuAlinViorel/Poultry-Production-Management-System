package com.ppms.shared.exception;

public class EntityNotFoundException extends DomainException {

    public EntityNotFoundException(String entity, Object id) {
        super("NOT_FOUND", entity + " not found: " + id);
    }
}
