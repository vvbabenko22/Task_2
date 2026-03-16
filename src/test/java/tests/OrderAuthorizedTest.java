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

// Указываем, что порядок тестов определяется аннотацией @Order
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
// Общие настройки для всех тестов
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrderAuthorizedTest {

    // Базовый URL
    private static String BASE_URL;

    // Контейнер для хранения данных пользователя
    private DataContainer dataContainer = new DataContainer();

    // Эндпоинты
    private final String REGISTER_ENDPOINT = "/auth/register";
    private final String DELETE_USER_ENDPOINT = "/auth/user";
    private final String CREATE_ORDER_ENDPOINT = "/orders";
    private final String GET_ORDERS_ENDPOINT = "/orders"; // эндпоинт для получения заказов

    // Генерация уникальных данных для регистрации
    private String generateUniqueEmail() {
        return new Faker().internet().emailAddress() + "_" + UUID.randomUUID().toString();
    }

    // Чтение конфигов перед тестами
    @BeforeAll
    public static void setup() throws IOException {
        Properties props = new Properties();
        InputStream input = OrderAuthorizedTest.class.getClassLoader().getResourceAsStream("config.properties");
        if (input != null) {
            props.load(input);
            BASE_URL = props.getProperty("base.url");
            RestAssured.baseURI = BASE_URL;
        } else {
            throw new RuntimeException("Файл config.properties не найден.");
        }
    }

    // Регистрация пользователя перед каждым тестом
    @BeforeEach
    public void registerUser() {
        // Генерируем уникальные данные для нового пользователя
        String uniqueEmail = generateUniqueEmail();
        String password = new Faker().internet().password();
        String name = new Faker().name().fullName();

        // Регистрируем нового пользователя
        CreateUserResponse registrationResponse = given()
                .contentType("application/json")
                .body(new CreateUserRequest(uniqueEmail, password, name))
                .when()
                .post(REGISTER_ENDPOINT)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .log().all()
                .extract().as(CreateUserResponse.class);

        // Сохраняем токен для последующего использования
        dataContainer.token = registrationResponse.getAccessToken();
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

    // Создание заказа с передачей токена
    @Step("Создание заказа с токеном")
    private OrderUnauthorizedResponse createOrderWithToken(OrderUnauthorizedRequest request, String token) {
        return given()
                .log().everything() // Полное логирование запроса
                .header("Authorization", token) // Используем токен без лишнего Bearer
                .contentType("application/json")
                .body(request)
                .when()
                .post(CREATE_ORDER_ENDPOINT)
                .then()
                .log().everything()
                .statusCode(200)
                .extract().as(OrderUnauthorizedResponse.class);
    }

    // Получение списка заказов с передачей токена
    @Step("Получение списка заказов с токеном")
    private GetOrdersResponse getOrdersWithToken(String token) {
        return given()
                .log().everything()
                .header("Authorization", token) // Используем токен без лишнего Bearer
                .when()
                .get(GET_ORDERS_ENDPOINT)
                .then()
                .log().everything()
                .statusCode(200)
                .extract().as(GetOrdersResponse.class);
    }

    // Авторизованный пользователь может создать заказ
    @Test
    @Order(1)
    @Description("Авторизованный пользователь может создать заказ")
    public void authorizedUserCanCreateOrder() {
        // Создаём заказ с использованием токена
        OrderUnauthorizedRequest orderRequest = new OrderUnauthorizedRequest(new String[]{"61c0c5a71d1f82001bdaaa6d", "61c0c5a71d1f82001bdaaa70", "61c0c5a71d1f82001bdaaa72"});
        OrderUnauthorizedResponse orderResponse = createOrderWithToken(orderRequest, dataContainer.token);

        // Проверяем успешность создания заказа
        assertEquals(true, orderResponse.isSuccess(), "Заказ не был успешно создан.");

        // Проверяем наличие всех необходимых полей в ответе
        assertNotNull(orderResponse.getName(), "Название заказа не установлено.");
        assertNotNull(orderResponse.getOrder(), "Поле 'order' отсутствует в ответе.");
        assertNotNull(orderResponse.getOrder().getIngredients(), "Список ингредиентов отсутствует.");
        assertNotNull(orderResponse.getOrder().getStatus(), "Статус заказа не установлен.");
        assertNotNull(orderResponse.getOrder().getCreatedAt(), "Время создания заказа не установлено.");
        assertNotNull(orderResponse.getOrder().getUpdatedAt(), "Время обновления заказа не установлено.");
        assertNotNull(orderResponse.getOrder().getNumber(), "Номер заказа не установлен.");
        assertNotNull(orderResponse.getOrder().getPrice(), "Цена заказа не установлена.");
        assertNotNull(orderResponse.getOrder().getOwner(), "Поле 'owner' отсутствует в ответе.");
    }

    // Тест: получение списка заказов с токеном
    @Test
    @Order(2)
    @Description("Авторизованный пользователь может получить список заказов")
    public void authorizedUserCanGetOrders() {
        // Получаем список заказов с использованием токена
        GetOrdersResponse ordersResponse = getOrdersWithToken(dataContainer.token);

        // Проверяем наличие всех необходимых полей в ответе
        assertNotNull(ordersResponse.getOrders(), "Список заказов отсутствует в ответе.");
        assertNotNull(ordersResponse.getTotal(), "Поле 'total' отсутствует в ответе.");
        assertNotNull(ordersResponse.getTotalToday(), "Поле 'totalToday' отсутствует в ответе.");

        // Проверяем наличие первого заказа в списке и параметров
        if (!ordersResponse.getOrders().isEmpty()) {
            OrderItem firstOrder = ordersResponse.getOrders().get(0);
            assertNotNull(firstOrder.get_id(), "ID первого заказа не установлен.");
            assertNotNull(firstOrder.getIngredients(), "Список ингредиентов первого заказа отсутствует.");
            assertNotNull(firstOrder.getStatus(), "Статус первого заказа не установлен.");
            assertNotNull(firstOrder.getName(), "Название первого заказа не установлено.");
            assertNotNull(firstOrder.getCreatedAt(), "Время создания первого заказа не установлено.");
            assertNotNull(firstOrder.getUpdatedAt(), "Время обновления первого заказа не установлено.");
            assertNotNull(firstOrder.getNumber(), "Номер первого заказа не установлен.");
        } else {
            System.out.println("Нет ни одного заказа в ответе.");
        }
    }

    // Вспомогательный класс для хранения данных пользователя
    private static class DataContainer {
        public String token = "";
    }
}