package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import test.models.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

class OrderUnauthorizedTest {

    // Базовый URL
    private static String BASE_URL;

    // Эндпоинты
    private final String CREATE_ORDER_ENDPOINT = "/orders";
    private final String FETCH_ORDERS_ENDPOINT = "/orders";

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
    private GetOrdersUnauthorizedResponse fetchOrders() {
        return given()
                .contentType("application/json")
                .when()
                .get(FETCH_ORDERS_ENDPOINT)
                .then()
                .statusCode(401)
                .log().all()
                .extract().as(GetOrdersUnauthorizedResponse.class);
    }

    // Тест создания нового заказа
    @Test
    @Description("Тест создания нового заказа")
    public void createNewOrder() {
        // Формирование запроса на создание заказа
        OrderUnauthorizedRequest requestBody = new OrderUnauthorizedRequest(new String[]{"61c0c5a71d1f82001bdaaa6d", "61c0c5a71d1f82001bdaaa70", "61c0c5a71d1f82001bdaaa72"});

        // Отправляем запрос и проверяем успешность
        OrderUnauthorizedResponse response = createOrder(requestBody);
        assertEquals(response.isSuccess(), true, "Заказ не был успешно создан.");

        // Проверяем наличие всех необходимых полей в ответе
        assertNotNull(response.getName(), "Название заказа не установлено.");
        assertNotNull(response.getOrder(), "Поле 'order' отсутствует в ответе.");
        assertNotNull(response.getOrder().getNumber(), "Номер заказа не установлен.");

        // Проверяем, что название заказа соответствует ожидаемому значению
        assertEquals("Метеоритный флюоресцентный spicy бургер", response.getName(), "Название заказа не соответствует ожидаемому значению.");
    }

    // Тест получения списка заказов
    @Test
    @Description("Тест получения списка заказов")
    public void fetchOrdersAndCheck() {
        // Получаем список заказов
        GetOrdersUnauthorizedResponse unauthorizedResponse = fetchOrders();

        // Проверяем, что запрос не был выполнен успешно
        assertFalse(unauthorizedResponse.isSuccess(), "Статус успеха не должен быть равен true.");

        // Проверяем наличие сообщения об ошибке
        assertEquals("You should be authorised", unauthorizedResponse.getMessage(), "Сообщение об ошибке не соответствует ожидаемому значению.");
    }

    // Создание заказа с некорректными ингредиентами
    @Test
    @Description("Негативный тест: создание заказа с некорректными ингредиентами")
    public void createOrderWithIncorrectIngredients() {
        // Подготовим запрос с некорректными ингредиентами
        OrderUnauthorizedRequest incorrectRequest = new OrderUnauthorizedRequest(new String[]{"61c0c5a71d1f82001bdaaa6d", "61c0c5a71d1f82001bdaaa70", "61c0c5a71d1f82001bdaaa7"}); // Третий хэш некорректный

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