Очередность запуска
1. Запустить сервер
2. На модуле NetworkChatClientServer -> mvn clean compile install
3. На модуле NetworlChatClient - javafx:compile & javafx:run




Создание Базы данных

CREATE TABLE users (
id       INTEGER PRIMARY KEY AUTOINCREMENT,
login    STRING  NOT NULL,
pass     STRING  NOT NULL,
userName STRING  NOT NULL
);