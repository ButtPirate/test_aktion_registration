package init;

import backend.TestConfig;
import backend.Tools;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import exceptions.BackendException;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.IOException;

import static backend.Backend.*;
import static backend.Tools.generateRandomString;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.*;
import static com.codeborne.selenide.Selenide.*;

public class Registration {
    static WebDriver driver;
    static String email;
    static String password;
    static String[] name;
    static String job;
    static String confirmationCode;
    static String confirmationLink;

    @BeforeSuite
    public static void setUp() throws BackendException {
        Tools.loadProperties();

        Configuration.timeout = TestConfig.TIMEOUT;
        Configuration.pageLoadStrategy = "normal";
        Configuration.savePageSource = false;
        driver = Tools.createDriver();
        driver.manage().window().maximize();
        WebDriverRunner.setWebDriver(driver);
        open(TestConfig.BASEURL);

    }

    @Test(description = "ЗАКРЫТИЕ ПРЕДУПРЕЖДЕНИЯ ОБ ИСПОЛЬЗОВАНИИ COOKIES")
    public static void Navigate_Cookies_Close() {
        $$(byText("Понятно")).findBy(visible).click();
    }

    @Test(description = "ПЕРЕХОД НА ВКЛАДКУ РЕГИСТРАЦИИ", dependsOnMethods = {"Navigate_Cookies_Close"})
    public static void Navigate_Tab_Registration() {
        $$(byClassName("auth__tab")).findBy(text("Зарегистрируйтесь")).click();
    }

    @Test(description = "ГЕНЕРАЦИЯ ДАННЫХ ДЛЯ РЕГИСТРАЦИИ", dependsOnMethods = {"Navigate_Tab_Registration"})
    public static void Data_Generate_RegData() throws BackendException {
        email = generateEmail();
        password = generateRandomString(8, "eng&digits");
        String[] generateFIO = {
                "Тестовский" + generateRandomString(3, "charsOnly"),
                "Тест" + generateRandomString(5, "charsOnly"),
                "Тестович" + generateRandomString(3, "charsOnly")
        };
        name = generateFIO;
        job = "Big Boss " + generateRandomString(5, "charsOnly");
        confirmationCode = generateMagicCode();

        System.out.println("email: " + email);
        System.out.println("password: " + password);
        for (String x : name) {
            System.out.println("Name[]: " + x);
        }
        System.out.println("job: " + job);
        System.out.println("confirmationCode: " + confirmationCode);

    }

    @Test(description = "ЗАПОЛНЕНИЕ ПОЛЕЙ НА ФОРМЕ РЕГИСТРАЦИИ", dependsOnMethods = {"Data_Generate_RegData"})
    public static void Fill_RegForm() {
        $$(byName("email")).findBy(visible).setValue(email);
        $$(byName("password")).findBy(visible).setValue(password);
        $$(byName("lastName")).findBy(visible).setValue(name[0]);
        $$(byName("firstName")).findBy(visible).setValue(name[1]);
        $$(byName("secondName")).findBy(visible).setValue(name[2]);
        $$(byName("mobilePhonePrefix")).findBy(visible).setValue(TestConfig.PHONE.split("-")[1]);
        $$(byName("mobilePhoneNumber")).findBy(visible).setValue(TestConfig.PHONE.split("-")[2]);
        $$(byName("code")).findBy(visible).setValue(confirmationCode);

    }

    @Test(description = "НАЖАТИЕ НА КНОПКУ \"Зарегистрироваться\"", dependsOnMethods = {"Fill_RegForm"})
    public static void Do_Register() {
        $$(byAttribute("value", "Зарегистрироваться")).findBy(visible).scrollTo().click();

        $(byText("Доступ предоставлен, осталось активировать")).waitUntil(visible, 30000);

    }

    @Test(description = "ЗАПРОС НА ПОЛУЧЕНИЕ ССЫЛКИ ДЛЯ ПОДТВЕРЖДЕНИЯ РЕГИСТРАЦИИ", dependsOnMethods = {"Do_Register"})
    public static void Data_GetConfirmationEmail() throws BackendException {
        confirmationLink = getConfirmationLink();

    }

    @Test(description = "ПЕРЕХОД ПО ССЫЛКЕ ДЛЯ ПОДТВЕРЖДЕНИЯ", dependsOnMethods = {"Data_GetConfirmationEmail"})
    public static void Navigate_ConfirmationLink() {
        open(confirmationLink);
    }

    @Test(description = "НАЖАТИЕ НА ССЫЛКУ \"Не хочу указывать\"", dependsOnMethods = {"Navigate_ConfirmationLink"})
    public static void Do_RefuseToSpecifyINN() {
        Selenide.sleep(3000);

        $$(byText("Не хочу указывать")).findBy(visible).click();

    }

    @Test(description = "ПРОВЕРКА УСПЕШНОГО ЛОГИНА", dependsOnMethods = {"Do_RefuseToSpecifyINN"})
    public static void Check_LoginSuccessful() {
        Selenide.sleep(3000);
        $(byText("Минуточку")).waitUntil(disappear, 30000);

        Assert.assertTrue($(byText("Демодоступ предоставлен")).is(visible));
        $(byId("btn-start-use")).click();
        Assert.assertTrue($(byId("user-enter")).has(text(name[1] + " " + name[0])));

        Selenide.sleep(5000);
    }

    @AfterSuite
    public static void tearDown() throws BackendException {
        if (Tools.httpclient != null) {
            try {
                Tools.httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new BackendException("Could not close HTTP client!");
            }
        }
        driver.quit();
    }

}
