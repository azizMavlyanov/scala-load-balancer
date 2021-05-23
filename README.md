# Load Balancer

## Author
Aziz Mavlyanov

## Stack
Scala, Akka HTTP

## Installation and usage of the project
1\) Clone the project:
```dotenv
git clone 
```
2\) Run the project from the root folder:
```dotenv
sbt run
```
3\) Make requests:
```dotenv
curl http://localhost:8080/balance?throughput=3
curl -X POST http://localhost:8080/end?server=192.168.0.1
```
**You can change configuration of servers in the file [servers.conf](https://github.com/azizMavlyanov/laravel-back)**



Задача: сделать http сервер на Scala который будет работать балансировщиком нагрузки, выделяя ресурсы серверов.
Для балансировки нагрузки в отдельном конфигурационном файле вводится несколько серверов, 
а так же максимальная пропускная способность каждого сервера. Например:

192.168.0.3 10
192.168.0.4 5
192.168.0.5 1

Для балансировки нагрузки, клиент может делать два запроса.
1.Выделить сервер для использования, с указанным количеством пропускной способности.
В ответ клиенту отдается ip сервера с доступной нужной пропускной способностью, или же сообщается, что такой сервер выделить невозможно.
2.Закончить использование сервера.

Примеры запросов, и ответов:
GET /balance?throughput=3
Ok 200 {server=”192.168.0.4”}
InternalServerError 500 {err=”No free server available”}

POST /end?server=192.168.0.4
Ok 200

Результат должен быть залит на github, и должен собираться с помощью команды sbt run.

Также можно получить дополнительные плюсы (опционально) за:

1. Наличие тестов
2. Понятный код
3.Обработку ошибок
4.Логирование
5.Быструю скорость работы
6.Отказоустойчивость
7.Не банальный алгоритм балансировки