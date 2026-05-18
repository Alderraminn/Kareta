

import ru.gr0946x.net.Server;
import ru.gr0946x.net.database.DatabaseManager;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        System.out.println(" Инициализация базы данных...");
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            System.out.println(" База данных готова!");
        } catch (Exception e) {
            System.err.println(" Ошибка инициализации БД: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        System.out.println(" Запуск сервера на порту 9460...");
        Server server = new Server(9460);
        System.out.println(" Сервер запущен и ожидает подключения клиентов");
    }
}