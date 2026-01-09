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
import test.models.CreateUserRequest;
import test.models.CreateUserResponse;
import test.models.UserInfo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

// Указываем, что порядок тестов определяется аннотацией @Order
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
// Общие настройки для всех тестов
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CreateUserTest {

    // Базовый URL
    private static String BASE_URL;

    // Контейнер для хранения данных пользователя
    private DataContainer dataContainer = new DataContainer();

    // Эндпоинты
    private final String REGISTER_ENDPOINT = "/auth/register";
    private final String DELETE_USER_ENDPOINT = "/auth/user";

    // Генерация уникальных данных для регистрации
    private String generateUniqueEmail() {
        return new Faker().internet().emailAddress() + "_" + UUID.randomUUID().toString();
    }

    // Чтение конфигов перед тестами
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

    // Удаление пользователя после всех тестов
    @AfterAll
    public void cleanup() {
        if (!dataContainer.token.isBlank()) {
            // Формируем заголовок Authorization
            String headerValue = dataContainer.token.startsWith("Bearer ") ? dataContainer.token : "Bearer " + dataContainer.token;

            given()
                    .header("Authorization", headerValue)
                    .delete(DELETE_USER_ENDPOINT)
                    .then()
                    .statusCode(202); // Ожидаем успешное удаление
            System.out.println("Пользователь успешно удалён!");
        }
    }

    // Проверка успешной регистрации пользователя
    @Test
    @Order(1) // Первым выполняется этот тест
    @Description("Пользователя можно успешно зарегистрировать")
    public void canCreateUserSuccessfully() {
        // Генерируем уникальные данные для регистрации
        String uniqueEmail = generateUniqueEmail();
        String password = new Faker().internet().password();
        String name = new Faker().name().fullName();

        // Регистрируем пользователя
        CreateUserResponse response = performRegistration(uniqueEmail, password, name);

        // Сохраняем имя и email из ответа метода
        dataContainer.registeredEmail = response.getUser().getEmail();
        dataContainer.registeredName = response.getUser().getName();
        dataContainer.token = response.getAccessToken();
    }

    // Невозможно создать двух одинаковых пользователей
    @Test
    @Order(2) // Вторым выполняется этот тест
    @Description("Невозможно создать двух одинаковых пользователей")
    public void cannotCreateDuplicateUsers() {
        // Повторная регистрация того же пользователя с существующими данными
        attemptDuplicateRegistration(dataContainer.registeredEmail, new Faker().internet().password(), dataContainer.registeredName);
    }

    // Проверка обязательности полей
    @Test
    @Order(3) // Третьим выполняется этот тест
    @Description("Необходимо передать все обязательные поля")
    public void allMandatoryFieldsAreRequired() {
        // Регистрация с отсутствующим обязательным параметром
        attemptIncompleteRegistration(generateUniqueEmail(), "", "");
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

    // Вспомогательный класс для хранения данных пользователя
    private static class DataContainer {
        public String registeredEmail = "";
        public String registeredName = "";
        public String token = "";
    }
}