# Kafka Streams Weather Analysis

Le projet **Kafka Streams Weather Analysis** est une application qui utilise la bibliothèque Kafka Streams pour traiter des données météorologiques en temps réel. L'application se connecte à un serveur Kafka, lit les messages du topic `weather-data`, applique un filtrage et un traitement de données en continu, puis produit des résultats agrégés dans un autre topic Kafka.

L'objectif de ce projet est de démontrer l'utilisation de Kafka Streams pour effectuer des transformations de flux de données, telles que le filtrage des données, l'agrégation et la conversion d'unités, tout en intégrant des pratiques de gestion des erreurs et des logs.

### Objectifs Principaux

1. **Collecte des données météorologiques** : L'application consomme des données météorologiques en temps réel via le topic Kafka `weather-data`.
2. **Filtrage des données** : Les données sont filtrées pour ne conserver que les enregistrements où la température est supérieure à 30°C.
3. **Conversion des températures** : Les températures, initialement exprimées en degrés Celsius, sont converties en degrés Fahrenheit.
4. **Agrégation des données** : L'application regroupe les données par station météorologique et calcule les moyennes de la température (en Fahrenheit) et de l'humidité.
5. **Publication des résultats** : Les résultats agrégés sont envoyés dans un nouveau topic Kafka `station-averages`, où ils peuvent être utilisés pour des analyses ou des visualisations ultérieures.

### Fonctionnalités Clés

- **Filtrage des enregistrements** : Le filtrage des données se fait en vérifiant si la température est supérieure à 30°C.
- **Conversion de la température** : Une fois filtrées, les températures sont converties de Celsius à Fahrenheit pour standardiser les données.
- **Agrégation par station** : Les données sont regroupées par station (identifiées par leur nom) et agrégées en calculant les moyennes des températures et de l'humidité.
- **Production des résultats** : Les résultats sont envoyés à un topic Kafka avec une description de la température moyenne et de l'humidité moyenne par station.

### Architecture du Projet

L'application Kafka Streams suit une architecture de traitement de flux basée sur les étapes suivantes :

1. **Lecture des données depuis Kafka** : L'application commence par lire des messages à partir du topic `weather-data`. Chaque message représente un enregistrement de données météorologiques (station, température en Celsius, humidité).
2. **Filtrage des données** : Un processus de filtrage est appliqué pour ne conserver que les données où la température est supérieure à 30°C.
3. **Transformation des données** : Les températures sont converties de Celsius en Fahrenheit.
4. **Agrégation par station** : Les données sont regroupées par station et les moyennes de température et d'humidité sont calculées.
5. **Écriture des résultats dans Kafka** : Les résultats agrégés (moyennes de température et d'humidité par station) sont envoyés à un topic Kafka `station-averages`.

### Traitement des Erreurs

L'application gère les erreurs courantes telles que :

- **Format de données invalide** : Si les données sont mal formatées ou si une valeur de température ou d'humidité ne peut pas être convertie, une exception est capturée et un message d'erreur est loggé sans que l'application ne se bloque.
- **Données manquantes** : Si des données sont manquantes (par exemple, une station sans température ou humidité), ces enregistrements sont ignorés.

### Flux de Travail

1. **Input** : L'application reçoit des données sous la forme de chaînes de caractères contenant trois informations : le nom de la station, la température en Celsius et l'humidité en pourcentage. 
   
   Exemple de message d'entrée :
   
    stationA, 32.5, 60


2. **Transformation** :
- Filtrage des données où la température est supérieure à 30°C.
- Conversion de la température de Celsius en Fahrenheit.

Exemple de transformation de données :  

stationA, 32.5, 60 → stationA, 90.5, 60

3. **Agrégation** :
- Les données sont agrégées par station, calculant la température moyenne et l'humidité moyenne sur une période donnée.

4. **Output** : Les résultats sont publiés dans un topic Kafka `station-averages`. 

Exemple de message de sortie :  

stationA, Température Moyenne = 89.5F, Humidité Moyenne = 62%

# Traitement d'application
## Le fichier pom.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>3.4.1</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>ma.banouidoha</groupId>
	<artifactId>kafka-streams-weather</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>kafka-streams-weather</name>
	<description>kafka-streams-weather</description>
	<url/>
	<licenses>
		<license/>
	</licenses>
	<developers>
		<developer/>
	</developers>
	<scm>
		<connection/>
		<developerConnection/>
		<tag/>
		<url/>
	</scm>
	<properties>
		<java.version>21</java.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter</artifactId>
		</dependency>
		<dependency>
			<groupId>org.apache.kafka</groupId>
			<artifactId>kafka-streams</artifactId>
			<version>3.8.0</version>
		</dependency>
		<dependency>
			<groupId>org.springframework.kafka</groupId>
			<artifactId>spring-kafka</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.kafka</groupId>
			<artifactId>spring-kafka-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>

</project>
```
## l'application
## Configuration de Kafka Streams
```java
Properties props = new Properties();
props.put("application.id", "weather-data-app");
props.put("bootstrap.servers", "localhost:9092");
props.put("default.key.serde", Serdes.String().getClass());
props.put("default.value.serde", Serdes.String().getClass());
```

## Lecture du flux de données depuis Kafka

    ```java
    KStream<String, String> weatherStream = builder.stream("weather-data");
    ```
    ## Filtrage des données
Les données sont filtrées pour ne conserver que celles dont la température est supérieure à 30°C.
```java
KStream<String, String> filteredWeatherStream = weatherStream.filter((key, value) -> {
    String[] parts = value.split(",");
    double temperature = Double.parseDouble(parts[1]);
    return temperature > 30;
});
```

## Conversion des températures
```java
KStream<String, String> convertedStream = filteredWeatherStream.mapValues(value -> {
    String[] parts = value.split(",");
    double celsius = Double.parseDouble(parts[1]);
    double fahrenheit = (celsius * 9 / 5) + 32;
    return parts[0] + "," + fahrenheit + "," + parts[2];
});
```
## Agrégation des données par station
```java
KGroupedStream<String, String> groupedStream = convertedStream.groupBy(
    (key, value) -> value.split(",")[0]
);
```
## Calcul des moyennes
```java
KTable<String, String> aggregatedTable = groupedStream.aggregate(
    () -> "0.0,0.0,0",  // Valeur initiale : température et humidité totales, et nombre de mesures
    (station, newValue, aggregate) -> {
        String[] newValueParts = newValue.split(",");
        String[] aggregateParts = aggregate.split(",");
        double newTemp = Double.parseDouble(newValueParts[1]);
        double newHumidity = Double.parseDouble(newValueParts[2]);
        double totalTemp = Double.parseDouble(aggregateParts[0]) + newTemp;
        double totalHumidity = Double.parseDouble(aggregateParts[1]) + newHumidity;
        int count = Integer.parseInt(aggregateParts[2]) + 1;
        return totalTemp + "," + totalHumidity + "," + count;
    }
);
```
## Création du flux de sortie

```java
KStream<String, String> resultStream = aggregatedTable.toStream().mapValues(aggregate -> {
    String[] parts = aggregate.split(",");
    double totalTemp = Double.parseDouble(parts[0]);
    double totalHumidity = Double.parseDouble(parts[1]);
    int count = Integer.parseInt(parts[2]);
    double avgTemp = totalTemp / count;
    double avgHumidity = totalHumidity / count;
    return "Température Moyenne = " + avgTemp + "F, Humidité Moyenne = " + avgHumidity + "%";
});
```
## Publication des résultats dans Kafka
```java
resultStream.to("station-averages", Produced.with(Serdes.String(), Serdes.String()));
```
## Démarrage et gestion de l'application Kafka Streams
```java
KafkaStreams streams = new KafkaStreams(builder.build(), props);
streams.start();
Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

```
# Kafka streams
## le fichier docker-compose
```yaml
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.7.1
    container_name: zookeeper
    hostname: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - 22181:2181

    networks:
      - kafka-net
  
  broker:
    image: confluentinc/cp-kafka:7.7.1
    container_name: broker
    hostname: broker
    depends_on:
      - zookeeper
    ports:
      - 9092:29092
    environment:
      KAFKA_BROKER_ID: 0
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENERS: INTERNAL://:9093,EXTERNAL_SAME_HOST://:29092
      KAFKA_ADVERTISED_LISTENERS: INTERNAL://broker:9093,EXTERNAL_SAME_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL_SAME_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    
    networks:
      - kafka-net


networks:
  kafka-net:
    driver: bridge
```
## Démarrage des services avec Docker Compose
Pour démarrer les services nécessaires au projet, utilisez la commande suivante dans le répertoire racine du projet :
```bash
docker-compose up -d
```



