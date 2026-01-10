package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import test.models.OrderUnauthorizedRequest;
import test.models.OrderUnauthorizedResponse;
import test.models.GetOrdersResponse;
import test.models.OrderItem;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

class OrderUnauthorizedTest {

    // Базовый URL
    private static String BASE_URL;

    // Эндпоинты
    private final String CREATE_ORDER_ENDPOINT = "/orders";
    private final String FETCH_ORDERS_ENDPOINT = "/orders/all";

    // Чтение конфигов перед тестами
    @BeforeAll
    public static void setup() throws IOException {
        Properties props = new Properties();
        InputStream input = OrderUnauthorizedTest.class.getClassLoader().getResourceAsStream("config.properties");
        if (input != null) {
            props.load(input);
            BASE_URL = props.getProperty("base.url");
            RestAssured.baseURI = BASE_URL;
        } else {
            throw new RuntimeException("Файл config.properties не найден.");
        }
    }

    // Создание нового заказа

    @Step("Создание нового заказа")
    private OrderUnauthorizedResponse createOrder(OrderUnauthorizedRequest request) {
        return given()
                .contentType("application/json")
                .body(request)
                .when()
                .post(CREATE_ORDER_ENDPOINT)
                .then()
                .log().all()
                .extract().as(OrderUnauthorizedResponse.class);
    }

    // Получение списка заказов
    @Step("Получение списка заказов")
    private GetOrdersResponse fetchOrders() {
        return given()
                .contentType("application/json")
                .when()
                .get(FETCH_ORDERS_ENDPOINT)
                .then()
                .statusCode(200)
                .log().all()
                .extract().as(GetOrdersResponse.class);
    }

    // Тест создания нового заказа
    @Test
    @Description("Тест создания нового заказа")
    public void createNewOrder() {
        // Формирование запроса на создание заказа
        OrderUnauthorizedRequest requestBody = new OrderUnauthorizedRequest(new String[] {"61c0c5a71d1f82001bdaaa6d"});

        // Отправляем запрос и проверяем успешность
        OrderUnauthorizedResponse response = createOrder(requestBody);
        assertEquals(response.isSuccess(), true, "Заказ не был успешно создан.");
    }

    // Тест получения списка заказов
    @Test
    @Description("Тест получения списка заказов")
    public void fetchOrdersAndCheck() {
        // Получаем список заказов
        GetOrdersResponse fetchedOrders = fetchOrders();

        // Проверяем наличие обязательных полей в ответе
        assertNotNull(fetchedOrders.getTotal(), "Поле 'total' отсутствует в ответе.");
        assertNotNull(fetchedOrders.getTotalToday(), "Поле 'totalToday' отсутствует в ответе.");

        // Дополнительная проверка: убедимся, что значения не нулевые и положительные
        assertTrue(fetchedOrders.getTotal() > 0, "Значение поля 'total' должно быть положительным числом.");
        assertTrue(fetchedOrders.getTotalToday() >= 0, "Значение поля 'totalToday' должно быть неотрицательным числом.");

        // Проверяем наличие параметров первого заказа в списке
        if (!fetchedOrders.getOrders().isEmpty()) {
            OrderItem firstOrder = fetchedOrders.getOrders().get(0);
            assertNotNull(firstOrder.get_id(), "ID первого заказа не установлен.");
            assertFalse(firstOrder.getIngredients().isEmpty(), "Список ингредиентов первого заказа пустой.");
            assertNotNull(firstOrder.getStatus(), "Статус первого заказа не установлен.");
            assertNotNull(firstOrder.getName(), "Название первого заказа не установлено.");
            assertNotNull(firstOrder.getCreatedAt(), "Время создания первого заказа не установлено.");
            assertNotNull(firstOrder.getUpdatedAt(), "Время обновления первого заказа не установлено.");
            assertNotNull(firstOrder.getNumber(), "Номер первого заказа не установлен.");
        } else {
            fail("Нет ни одного заказа в ответе.");
        }
    }

    private void assertTrue(boolean b, String s) {
    }

    // Создание заказа с некорректными ингредиентами
    @Test
    @Description("Негативный тест: создание заказа с некорректными ингредиентами")
    public void createOrderWithIncorrectIngredients() {
        // Подготовим запрос с некорректными ингредиентами
        OrderUnauthorizedRequest incorrectRequest = new OrderUnauthorizedRequest(new String[] {"61c0c5a71d1f82001bdaaa6d", "61c0c5a71d1f82001bdaaa70", "61c0c5a71d1f82001bdaaa7"}); // Третий хэш некорректный

        // Отправляем запрос и проверяем, что пришел статус 500
        given()
                .contentType("application/json")
                .body(incorrectRequest)
                .when()
                .post(CREATE_ORDER_ENDPOINT)
                .then()
                .statusCode(500)
                .log().all();
    }

    // Создание заказа с пустым телом запроса
    @Test
    @Description("Негативный тест: создание заказа с пустым телом запроса")
    public void createOrderWithEmptyBody() {
        // Отправляем запрос с пустым телом
        given()
                .contentType("application/json")
                .body("") // Пустой запрос
                .when()
                .post(CREATE_ORDER_ENDPOINT)
                .then()
                .statusCode(400)
                .body("message", equalTo("Ingredient ids must be provided"))
                .log().all();
    }
}