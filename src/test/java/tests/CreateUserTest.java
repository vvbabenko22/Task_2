package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Assertions;
import com.github.javafaker.Faker;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;
import test.models.CreateUserRequest;
import test.models.CreateUserResponse;
import test.models.UserInfo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

// Общий класс настроек для всех тестов
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CreateUserTest {

    // Базовый URL
    private static String BASE_URL;

    // Эндпоинты
    private final String REGISTER_ENDPOINT = "/auth/register";
    private final String DELETE_USER_ENDPOINT = "/auth/user";

    // Генерация уникальных данных для регистрации
    private String generateUniqueEmail() {
        return new Faker().internet().emailAddress() + "_" + UUID.randomUUID().toString();
    }

    // Чтение конфигов перед всеми тестами
    @BeforeAll
    public static void setup() throws IOException {
        Properties props = new Properties();
        InputStream input = CreateUserTest.class.getClassLoader().getResourceAsStream("config.properties");
        if (input != null) {
            props.load(input);
            BASE_URL = props.getProperty("base.url");
            RestAssured.baseURI = BASE_URL;
        } else {
            throw new RuntimeException("Файл config.properties не найден.");
        }
    }

    // Очистка после каждого теста
    @AfterEach
    public void cleanup() {
        try {
            DeleteRegisteredUser();
        } catch (Throwable e) {
            // Игнорируем исключение, если оно возникло при удалении пользователя
        }
    }

    // Проверка успешной регистрации пользователя
    @Test
    @Description("Пользователя можно успешно зарегистрировать")
    public void canCreateUserSuccessfully() {
        // Создаем уникальный аккаунт
        String uniqueEmail = generateUniqueEmail();
        String password = new Faker().internet().password();
        String name = new Faker().name().fullName();

        // Регистрируемся
        CreateUserResponse response = performRegistration(uniqueEmail, password, name);

        // Удостоверимся, что токен и данные были получены
        Assertions.assertFalse(response.getAccessToken().isEmpty());
        Assertions.assertEquals(uniqueEmail, response.getUser().getEmail());
        Assertions.assertEquals(name, response.getUser().getName());
    }

    // Невозможно создать двух одинаковых пользователей
    @Test
    @Description("Невозможно создать двух одинаковых пользователей")
    public void cannotCreateDuplicateUsers() {
        // Создаем уникальный аккаунт
        String uniqueEmail = generateUniqueEmail();
        String password = new Faker().internet().password();
        String name = new Faker().name().fullName();

        // Сначала регистрируем пользователя
        performRegistration(uniqueEmail, password, name);

        // Пробуем повторить регистрацию с теми же данными
        attemptDuplicateRegistration(uniqueEmail, password, name);
    }

    // Проверка обязательности полей
    @Test
    @Description("Необходимо передать все обязательные поля")
    public void allMandatoryFieldsAreRequired() {
        // Пробуем зарегистрироваться с пустым паролем и именем
        attemptIncompleteRegistration(generateUniqueEmail(), "", "");
    }

    // Метод удаления зарегистрированного пользователя
    private void DeleteRegisteredUser() {
        CreateUserResponse lastUserData = performRegistration("", "", "");
        if (!lastUserData.getAccessToken().isEmpty()) { // проверка, есть ли токен
            String headerValue = lastUserData.getAccessToken().startsWith("Bearer ") ?
                    lastUserData.getAccessToken() :
                    "Bearer " + lastUserData.getAccessToken();

            given()
                    .header("Authorization", headerValue)
                    .delete(DELETE_USER_ENDPOINT)
                    .then()
                    .statusCode(202); // ожидаем успешного удаления
        }
    }

    // Регистрация пользователя
    @Step("Регистрация пользователя")
    private CreateUserResponse performRegistration(String email, String password, String name) {
        CreateUserRequest request = new CreateUserRequest(email, password, name);
        CreateUserResponse response = given()
                .contentType("application/json")
                .body(request)
                .when()
                .post(REGISTER_ENDPOINT)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .log().all()
                .extract().as(CreateUserResponse.class);

        return response;
    }

    // Повторная регистрация пользователя
    @Step("Повторная регистрация пользователя")
    private void attemptDuplicateRegistration(String email, String password, String name) {
        CreateUserRequest request = new CreateUserRequest(email, password, name);
        given()
                .contentType("application/json")
                .body(request)
                .when()
                .post(REGISTER_ENDPOINT)
                .then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("User already exists"));
    }

    // Регистрация с неполными данными
    @Step("Регистрация с неполными данными")
    private void attemptIncompleteRegistration(String email, String password, String name) {
        CreateUserRequest request = new CreateUserRequest(email, password, name);
        given()
                .contentType("application/json")
                .body(request)
                .when()
                .post(REGISTER_ENDPOINT)
                .then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("Email, password and name are required fields"));
    }
}