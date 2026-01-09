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
import test.models.UpdateUserRequest;
import test.models.UpdateUserResponse;
import test.models.UserInfo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Общие настройки для всех тестов
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UpdateUserTest {

    // Базовый URL
    private static String BASE_URL;

    // Контейнер для хранения данных пользователя
    private DataContainer dataContainer = new DataContainer();

    // Эндпоинты
    private final String REGISTER_ENDPOINT = "/auth/register";
    private final String UPDATE_PROFILE_ENDPOINT = "/auth/user";
    private final String DELETE_USER_ENDPOINT = "/auth/user";

    // Генерация уникальных данных для регистрации
    private String generateUniqueEmail() {
        return new Faker().internet().emailAddress() + "_" + UUID.randomUUID().toString();
    }

    // Чтение конфигов перед тестами
    @BeforeAll
    public static void setup() throws IOException {
        Properties props = new Properties();
        InputStream input = UpdateUserTest.class.getClassLoader().getResourceAsStream("config.properties");
        if (input != null) {
            props.load(input);
            BASE_URL = props.getProperty("base.url");
            RestAssured.baseURI = BASE_URL;
        } else {
            throw new RuntimeException("Файл config.properties не найден.");
        }
    }

    // Регистрация нового пользователя перед каждым тестом
    @BeforeEach
    public void beforeEach() {
        // Генерируем уникальные данные для нового пользователя
        String uniqueEmail = generateUniqueEmail();
        String password = new Faker().internet().password();
        String name = new Faker().name().fullName();

        // Регистрируем нового пользователя и получаем токен
        CreateUserResponse registrationResponse = registerNewUser(uniqueEmail, password, name);
        dataContainer.token = registrationResponse.getAccessToken(); // Берём токен из ответа регистрации
        dataContainer.email = uniqueEmail; // Сохраняем адрес электронной почты
        dataContainer.name = name; // Сохраняем имя пользователя
    }

    // Удаление пользователя после каждого теста
    @AfterEach
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

    // Попытка изменения данных без авторизации
    @Test
    @Description("Редактирование данных без авторизации недопустимо")
    public void changeDataWithoutAuth() {
        // Пробуем обновить данные без авторизации
        UpdateUserRequest updateRequest = new UpdateUserRequest(
                "new-" + dataContainer.email, // Используем сохранённое значение email
                "new-password",
                "new-" + dataContainer.name // Используем сохранённое значение name
        );

        given()
                .contentType("application/json")
                .body(updateRequest)
                .patch(UPDATE_PROFILE_ENDPOINT)
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    // Редактирование данных с авторизацией
    @Test
    @Description("Редактирование данных с авторизацией проходит успешно")
    public void changeDataWithAuth() {
        // Готовим данные для обновления
        String updatedEmail = "updated-" + dataContainer.email;
        String updatedPassword = "updated-password";
        String updatedName = "updated-" + dataContainer.name;

        UpdateUserRequest updateRequest = new UpdateUserRequest(updatedEmail, updatedPassword, updatedName);

        // Отправляем запрос на обновление данных с авторизацией
        UpdateUserResponse updateResponse = given()
                .log().everything() // Полное логирование запроса
                .header("Authorization", dataContainer.token) // Убираем лишний Bearer
                .contentType("application/json")
                .body(updateRequest)
                .when()
                .patch(UPDATE_PROFILE_ENDPOINT)
                .then()
                .log().everything() // Полное логирование ответа
                .statusCode(200)
                .body("success", equalTo(true))
                .extract().as(UpdateUserResponse.class);

        // Проверяем, что данные пользователя обновились
        assertEquals(updateResponse.getUser().getEmail(), updatedEmail);
        assertEquals(updateResponse.getUser().getName(), updatedName);
    }

    // Регистрация нового пользователя
    @Step("Регистрация нового пользователя")
    private CreateUserResponse registerNewUser(String email, String password, String name) {
        CreateUserRequest request = new CreateUserRequest(email, password, name);
        return given()
                .contentType("application/json")
                .body(request)
                .when()
                .post(REGISTER_ENDPOINT)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .extract().as(CreateUserResponse.class);
    }

    // Вспомогательные классы для хранения данных пользователя
    private static class DataContainer {
        public String token = ""; // Токен для удаления пользователя
        public String email = ""; // Электронная почта пользователя
        public String name = ""; // Имя пользователя
    }
}