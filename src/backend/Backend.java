package backend;

import exceptions.BackendException;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stef6 on 04/15/2018.
 */
public class Backend {
    /**
     * Создает почтовый ящик на temp-mail.org, сохраняет cookies для последующего использования.
     *
     * @return - адрес созданного почтового ящика
     * @throws BackendException - если не получилось выполнить запрос или если не удалось найти элемент, содержащий адрес, в ответе
     */
    public static String generateEmail() throws BackendException {
        String result = Tools.executeGetRequest("https://temp-mail.org/en/");

        Document document = Jsoup.parse(result);
        Element mailElement = document.selectFirst("#mail");
        if (mailElement == null) {
            throw new BackendException("Could not find mailbox element in response!");
        }

        return mailElement.attr("value");
    }

    /**
     * Полный метод для генерации "магического" кода подтверждения по мобильному телефону.
     *
     * @return - код
     * @throws BackendException - если что-то пошло не так при логине в бэкэнд офис или при запросе на код
     */
    public static String generateMagicCode() throws BackendException {
        loginToSGCRM(TestConfig.SGCRM_LOGIN, TestConfig.SGCRM_PASSWORD);
        return requestMagicCode();

    }

    /**
     * Логинится в бэкенд кабинет и сохраняет cookies
     *
     * @param login    - логин для входа
     * @param password - пароль для входа
     * @throws BackendException - если при выполнении запроса что-то пошло не так.
     */
    private static void loginToSGCRM(String login, String password) throws BackendException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("login", login));
        params.add(new BasicNameValuePair("pass", password));

        Tools.executePostRequest("http://sgcrm-rt.actiondigital.ru/login.aspx", params, null);

    }

    /**
     * Запрашивает "Волшебный код" для продтвержжедения регистрации по СМС.
     * Подразумевается, что уже был произведен логин в бэкэнд офис через метод loginToSGCRM
     *
     * @return - вернувшийся код
     * @throws BackendException - если что-то пошло не так при запросе, или если не получилось найти элемент с цифрами на вернувшейся странице.
     */
    private static String requestMagicCode() throws BackendException {
        List<NameValuePair> additionalHeaders = new ArrayList<NameValuePair>();
        additionalHeaders.add(new BasicNameValuePair("Content-Type", "application/x-www-form-urlencoded"));
        additionalHeaders.add(new BasicNameValuePair("Upgrade-Insecure-Requests", "1"));

        String magicPage = Tools.executePostRequest("http://sgcrm-rt.actiondigital.ru/magic/magic.aspx", null, additionalHeaders);
        Document document = Jsoup.parse(magicPage);

        Element magicNumbers = document.selectFirst("div[style=font-weight: bold; font-size: 16pt; padding-top: 15px;]");
        if (magicNumbers == null) {
            throw new BackendException("Could not find element with magic numbers on the returned page!");
        }
        return magicNumbers.text();

    }

    /**
     * Получает из почты ссылку на подтверждение.
     * Подразумевается, что почта уже была создана, и что письмо было выслано.
     * Ждет минуту до появления письма, повторяя запрос каждые две секунды.
     *
     * @return - ссылка на подтверждение регистрации
     * @throws BackendException - если что-то пошло не так при запросах, если не дождался письма за 60 сек, если не смог найти в письме ссылку.
     */
    public static String getConfirmationLink() throws BackendException {
        Element confirmationMail = null;
        int tries = 0;
        while (confirmationMail == null) {
            tries++;
            String inboxPage = Tools.executeGetRequest("https://temp-mail.org/en/");
            Document inboxDocument = Jsoup.parse(inboxPage);
            confirmationMail = inboxDocument.selectFirst("[href^=https://temp-mail.org/en/view/]");
            System.out.println("No mail yet! I'll try again in a bit...");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new BackendException("Could not pause thread while waiting for confirmation mail!");
            }

            if (tries > 30) {
                throw new BackendException("Could not get confirmation email in 60 seconds!");
            }

        }
        String mailLink = confirmationMail.attr("href");

        String mailPage = Tools.executeGetRequest(mailLink);
        Document mailDoc = Jsoup.parse(mailPage);
        Element confLink = mailDoc.selectFirst("[href^=http://uss-rt.actiondigital.ru/_/registration/confirm/]");
        if (confLink == null) {
            throw new BackendException("Could not find confirmation link in the recieved email!");
        }
        String confirmationLink = confLink.attr("href");
        return confirmationLink;
    }

}
