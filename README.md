# Load Balancer

## Author
Aziz Mavlyanov

## Stack
Scala, Akka HTTP

## Installation and usage of the project
1\) Clone the project:
```dotenv
git clone git@github.com:azizMavlyanov/scala-load-balancer.git
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
**You can change configuration of servers in the file [servers.conf](https://github.com/azizMavlyanov/scala-load-balancer/blob/master/src/main/resources/servers.conf)**
