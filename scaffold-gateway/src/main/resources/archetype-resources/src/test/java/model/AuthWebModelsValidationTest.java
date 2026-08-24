package ${package}.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthWebModelsValidationTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Test
    void shouldValidateRefreshTokenBounds() {
        assertTrue(VALIDATOR.validate(new AuthWebModels.RefreshRequest("v4.refresh-token", "web", "device")).isEmpty());
        assertFalse(VALIDATOR.validate(new AuthWebModels.RefreshRequest("", "web", "device")).isEmpty());
        assertFalse(VALIDATOR.validate(new AuthWebModels.RefreshRequest("x".repeat(257), "web", "device")).isEmpty());
    }
}
