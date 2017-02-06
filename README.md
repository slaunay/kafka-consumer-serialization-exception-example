# Consumer SerializationException infinite loop

Kafka 0.10.x consumer code to reproduce infinite loop when a topic contains a
malformed key or value but the `Deserializer` throws a `SerializationException`.

## Requirements

To run the examples you will need:

- [JDK 1.8+](http://www.oracle.com/technetwork/java/javase/downloads)

## Configure the topic and publish records

Create the topic:

    bin/kafka-topics.sh --zookeeper localhost:2181 --create --topic test --partitions 1 --replication-factor 1

Then publish a few records (int32 values) including one malformed integer value:

    printf "\x00\x00\x00\x00\n" | bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test
    printf "\x00\x00\x00\x01\n" | bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test
    printf "\x00\x00\x00\n"     | bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test
    printf "\x00\x00\x00\x02\n" | bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test

## Run the consumer with Kafka clients 0.10.1.1

Use the Gradle wrapper (no need to install Gradle) to build and run the application:

    ./gradlew run

## Run the consumer with Kafka clients 0.10.0.1

Use the Gradle wrapper (no need to install Gradle) to build and run the application:

    ./gradlew run -DkafkaVersion=0.10.0.1

## Run the consumer with Kafka clients 0.9.0.1

Use the Gradle wrapper (no need to install Gradle) to build and run the application:

    ./gradlew run -DkafkaVersion=0.9.0.1
