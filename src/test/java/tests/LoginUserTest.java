package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import com.github.javafaker.Faker;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import test.models.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

// Общие настройки для всех тестов
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LoginUserTest {

    // Базовый URL
    private static String BASE_URL;

    // Контейнер для хранения данных пользователя
    private DataContainer dataContainer = new DataContainer();

    // Эндпоинты
    private final String REGISTER_ENDPOINT = "/auth/register";
    private final String LOGIN_ENDPOINT = "/auth/login";
    private final String DELETE_USER_ENDPOINT = "/auth/user";

    // Генерация уникальных данных для регистрации
    private String generateUniqueEmail() {
        return new Faker().internet().emailAddress() + "_" + UUID.randomUUID().toString();
    }

    // Чтение конфигов перед тестами
    @BeforeAll
    public static void setup() throws IOException {
        Properties props = new Properties();
        InputStream input = LoginUserTest.class.getClassLoader().getResourceAsStream("config.properties");
        if (input != null) {
            props.load(input);
            BASE_URL = props.getProperty("base.url");
            RestAssured.baseURI = BASE_URL;
        } else {
            throw new RuntimeException("Файл config.properties не найден.");
        }
    }

    // Регистрация пользователя перед каждым тестом
    @Step("Регистрация пользователя")
    @BeforeEach
    public void registerUser() {
        // Генерируем уникальные данные для нового пользователя
        String uniqueEmail = generateUniqueEmail();
        String password = new Faker().internet().password();
        String name = new Faker().name().fullName();

        // Регистрируем нового пользователя
        given()
                .contentType("application/json")
                .body(new CreateUserRequest(uniqueEmail, password, name))
                .when()
                .post(REGISTER_ENDPOINT)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        // Сохраняем данные пользователя для последующего использования
        dataContainer.email = uniqueEmail;
        dataContainer.password = password;
    }

    // Удаление пользователя после всех тестов
    @AfterAll
    public void cleanup() {
        if (!dataContainer.token.isBlank()) {
            // Формируем заголовок Authorization
            String headerValue = dataContainer.token.startsWith("Bearer ")
                    ? dataContainer.token
                    : "Bearer " + dataContainer.token;

            given()
                    .header("Authorization", headerValue)
                    .delete(DELETE_USER_ENDPOINT)
                    .then()
                    .statusCode(202); // Ожидаем успешное удаление
            System.out.println("Пользователь успешно удалён!");
        }
    }

    // Авторизация пользователя с неверными данными
    @Test
    @Description("Авторизация невозможна с неверными данными")
    public void invalidLoginAttempt() {
        // Данные для неудачной попытки авторизации
        String wrongEmail = "wrong@example.com";
        String wrongPassword = "wrong_password";

        // Отправляем запрос на авторизацию и проверяем статус-код и тело ответа
        given()
                .contentType("application/json")
                .body(new LoginRequest(wrongEmail, wrongPassword))
                .when()
                .post(LOGIN_ENDPOINT)
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("email or password are incorrect"));
    }

    // Создание пользователя и успешная авторизация
    @Test
    @Description("Можно успешно войти под новым пользователем")
    @Step("Авторизация пользователя")
    public void successfulLoginAfterRegistration() {
        // Авторизуемся под новым пользователем
        LoginResponse loginResponse = given()
                .contentType("application/json")
                .body(new LoginRequest(dataContainer.email, dataContainer.password))
                .when()
                .post(LOGIN_ENDPOINT)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .extract().as(LoginResponse.class);

        // Проверяем полученные данные пользователя
        assertNotNull(loginResponse.getAccessToken());
        assertNotNull(loginResponse.getRefreshToken());
        assertEquals(loginResponse.getUser().getEmail(), dataContainer.email);

        // Сохраняем токен для последующего удаления пользователя
        dataContainer.token = loginResponse.getAccessToken();
    }

    // Вспомогательный класс для хранения данных пользователя
    private static class DataContainer {
        public String email = "";
        public String password = "";
        public String token = "";
    }
}