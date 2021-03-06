// Copyright © 2015 HSL <https://www.hsl.fi>
// This program is dual-licensed under the EUPL v1.2 and AGPLv3 licenses.

package fi.hsl.parkandride.itest;

import com.jayway.restassured.http.ContentType;
import fi.hsl.parkandride.core.domain.Facility;
import fi.hsl.parkandride.core.domain.NewUser;
import fi.hsl.parkandride.core.domain.Role;
import fi.hsl.parkandride.core.service.ValidationException;
import fi.hsl.parkandride.front.UrlSchema;
import org.junit.Test;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static com.jayway.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;

public class ErrorHandlingITest extends AbstractIntegrationTest {
    @Test
    public void non_existing_resource_is_communicated_with_status_code_only() {
        when()
            .get(UrlSchema.FACILITIES + "/42")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body(is(""))
        ;
    }

    @Test
    public void unauthorized_access() {
        givenWithContent()
            .body(new Facility())
        .when()
            .post(UrlSchema.FACILITIES)
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void validation_errors_are_detailed_as_violations() {
        devHelper.createOrUpdateUser(new NewUser(1l, "admin", Role.ADMIN, "admin"));
        givenWithContent(devHelper.login("admin").token)
            .body(new Facility())
        .when()
            .post(UrlSchema.FACILITIES)
        .then()
            .spec(assertResponse(HttpStatus.BAD_REQUEST, ValidationException.class))
            .contentType(ContentType.JSON)
            .body("message", startsWith("Invalid data. Violations in"))
            .body("violations", is(notNullValue()))
        ;
    }

    @Test
    public void typical_json_mapping_violations_are_detected() {
        givenWithContent()
            .body(resourceAsString("facility.create.JsonMappingException.json"))
        .when()
            .post(UrlSchema.FACILITIES)
        .then()
            .spec(assertResponse(HttpStatus.BAD_REQUEST, HttpMessageNotReadableException.class))
            .body("violations[0].path", is("builtCapacity.CAR"))
        ;
    }

    @Test
    public void geolatte_json_mapping_violations_are_detected() {
        givenWithContent()
            .body("{ \"name\": \"foo\", \"location\": \"this is not readable location\"  }")
        .when()
            .post(UrlSchema.FACILITIES)
        .then()
            .spec(assertResponse(HttpStatus.BAD_REQUEST, HttpMessageNotReadableException.class))
            .body("violations[0].path", is("location"))
        ;
    }

    @Test
    public void unsupported_request_methods_are_detected() {
        givenWithContent()
            .body(new Facility())
        .when()
            .put(UrlSchema.FACILITIES)
        .then()
            .spec(assertResponse(HttpStatus.BAD_REQUEST, HttpRequestMethodNotSupportedException.class))
            .body("message", is("Request method 'PUT' not supported"))
        ;
    }

    @Test
    public void request_parameter_binding_errors_are_detailed_as_violations() {
        when()
            .get(UrlSchema.FACILITIES + "?ids=foo")
        .then()
            .spec(assertResponse(HttpStatus.BAD_REQUEST, BindException.class))
            .body("message", is("Invalid request parameters"))
            .body("violations[0].path", is("ids"))
            .body("violations[0].message", is("Failed to convert property value of type 'java.lang.String' to required type 'java.util.Set' for property " +
                    "'ids'; nested exception is java.lang.NumberFormatException: For input string: \"foo\""))
        ;
    }

    @Test
    public void invalid_path_variable_type_is_reported_as_internal_error() {
        when()
            .get(UrlSchema.FACILITIES + "/foo")
        .then()
            .spec(assertResponse(HttpStatus.INTERNAL_SERVER_ERROR, TypeMismatchException.class))
            .body("message", is("Failed to convert value of type 'java.lang.String' to required type 'long'; nested exception is " +
                    "java.lang.NumberFormatException: For input string: \"foo\""))
            .body("violations", is(nullValue()))
        ;
    }

    // HttpMediaTypeException: unclear how to trigger
}
