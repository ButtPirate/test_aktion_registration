package backend;

import exceptions.BackendException;
import init.Registration;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;

/**
 * Created by Stef6 on 04/15/2018.
 */
public class Tools {
    static BasicCookieStore cookieStore = new BasicCookieStore();
    public static CloseableHttpClient httpclient = null;

    /**
     * Создает драйвер для браузера chrome
     *
     * @return
     */
    public static WebDriver createDriver() {
        System.setProperty("webdriver.chrome.driver", "lib\\chromedriver.exe");
        DesiredCapabilities caps = DesiredCapabilities.chrome();
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        caps.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);
        return new ChromeDriver(caps);
    }

    /**
     * Генерирует String со случайными символами.
     *
     * @param length - длинна требуемой строчки
     * @param mode   - режим генерации: charsOnly, digitsOnly, chars&digits, eng&digits
     * @return
     */
    public static String generateRandomString(int length, String mode) {
        String characters;
        switch (mode) {
            case "charsOnly":
                characters = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
                break;
            case "digitsOnly":
                characters = "123456789";
                break;
            case "chars&digits":
                characters = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя0123456789";
                break;
            case "eng&digits":
                characters = "abcdefghijklmnopqrstuvwxyz0123456789";
                break;
            default:
                characters = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
                break;
        }

        Random rng = new Random();
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);

    }

    /**
     * Выполняет GET-запрос
     *
     * @param URI - адрес, на который нужно послать запрс
     * @return - содержимое ответа
     * @throws BackendException - если не получилось запарсить ответ, или выполнить запрос
     */
    static String executeGetRequest(String URI) throws BackendException {
        if (httpclient == null) {
            httpclient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
        }

        HttpGet httpget = new HttpGet(URI);

        String parsedResponse = "";

        try (CloseableHttpResponse response = httpclient.execute(httpget)) {
            parsedResponse = parseRequestResponse(response.getEntity());
        } catch (IOException e) {
            e.printStackTrace();
            throw new BackendException("Could not execute GET-request to <" + URI + ">!");
        }

        return parsedResponse;

    }

    /**
     * Выполняет POST-запрос.
     * Можно указать параметры для отсылки и дополнительные заголовки.
     *
     * @param URI               - адрес, на который будет посылаться запрос
     * @param params            - список параметров, Nullable
     * @param additionalHeaders - список дополнительных заголовков, Nullable
     * @return - содержимое ответа
     * @throws BackendException - если не удалось выполнить запрос, распарсить ответ, или если не получилось перекодировать параметры.
     */
    static String executePostRequest(String URI, List<NameValuePair> params, List<NameValuePair> additionalHeaders) throws BackendException {
        if (httpclient == null) {
            httpclient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
        }

        HttpPost httpPost = new HttpPost(URI);

        if (params != null) {
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(params));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new BackendException("Problems with encoding of POST- request <" + URI + ">!");
            }
        }

        if (additionalHeaders != null) {
            for (NameValuePair x : additionalHeaders) {
                httpPost.setHeader(x.getName(), x.getValue());
            }
        }

        String parsedResponse;

        try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
            parsedResponse = parseRequestResponse(response.getEntity());
        } catch (IOException e) {
            e.printStackTrace();
            throw new BackendException("Could not execute POST- request <" + URI + ">!");
        }

        return parsedResponse;


    }

    /**
     * Парсит HTTPEntity в String
     *
     * @param entity
     * @return
     * @throws BackendException - если при чтении или закрытии потоков произошла ошибка.
     */
    private static String parseRequestResponse(HttpEntity entity) throws BackendException {
        StringBuffer result = new StringBuffer();

        try {
            InputStreamReader is = new InputStreamReader(entity.getContent(), "UTF-8");
            BufferedReader rd = new BufferedReader(is);

            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            EntityUtils.consume(entity);
            is.close();
            rd.close();

        } catch (Exception e) {
            e.printStackTrace();
            throw new BackendException("Could not read contents of response entity!");

        }

        return result.toString();

    }

    /**
     * Читает параметры теста из файла test.properties и проставляет их в поля класса TestConfig.
     * Файл должен располагаться в папке ресурсов (в соответствии с pom - можно в корне src)
     *
     * @return - объект Properties
     * @throws BackendException - если что-то пошло не так при чтении или закрытии потока
     */
    public static void loadProperties() throws BackendException {
        InputStream is = Registration.class.getClassLoader().getResourceAsStream("test.properties");
        Properties p = new Properties();
        try {
            p.load(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new BackendException("Autotest could not read property file \"test.properties\"!");
        }

        TestConfig.TIMEOUT = Integer.parseInt(p.getProperty("timeout"));
        TestConfig.BASEURL = p.getProperty("baseurl");
        TestConfig.SGCRM_LOGIN = p.getProperty("sgcrm_login");
        TestConfig.SGCRM_PASSWORD = p.getProperty("sgcrm_password");
        TestConfig.PHONE = p.getProperty("phone");

    }
}
