package ma.banouidoha.kafkastreamsweather;

import java.util.Properties;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.common.serialization.Serdes;

// Cette application collecte et analyse les données météorologiques en temps réel via Kafka Streams.
public class WeatherAnalysisApp {
	public static void main(String[] args) {
		// Configuration des propriétés de l'application Kafka Streams
		Properties props = new Properties();
		props.put("application.id", "weather-data-app");  // Identifiant unique pour l'application Kafka Streams
		props.put("bootstrap.servers", "localhost:9092"); // Adresse du serveur Kafka
		props.put("default.key.serde", Serdes.String().getClass());  // Sérialiseur/désérialiseur pour les clés (type String)
		props.put("default.value.serde", Serdes.String().getClass());  // Sérialiseur/désérialiseur pour les valeurs (type String)

		// Création d'un builder Kafka Streams pour définir la logique de traitement des flux
		StreamsBuilder builder = new StreamsBuilder();

		// Lecture du topic Kafka "weather-data", qui contient les données météorologiques
		KStream<String, String> weatherStream = builder.stream("weather-data");

		// Filtrage des données où la température est supérieure à 30°C
		KStream<String, String> filteredWeatherStream = weatherStream.filter((key, value) -> {
			try {
				// Séparation de la chaîne de caractères en différentes parties : station, température, humidité
				String[] parts = value.split(",");
				if (parts.length < 3) return false;  // Vérification que les données sont complètes
				double temperature = Double.parseDouble(parts[1]);  // Conversion de la température en double
				boolean passesFilter = temperature > 30;  // Filtrage : conserver les données où la température > 30°C
				System.out.println("Filtered: " + value + " -> " + passesFilter);  // Affichage des données filtrées
				return passesFilter;  // Retourne true si la température dépasse 30°C
			} catch (Exception e) {
				System.err.println("Error processing record: " + value + " - " + e.getMessage());
				return false;  // Ignorer les enregistrements non conformes ou mal formatés
			}
		});

		// Conversion de la température en Fahrenheit
		KStream<String, String> convertedStream = filteredWeatherStream.mapValues(value -> {
			String[] parts = value.split(",");
			String station = parts[0];  // Nom de la station
			double celsius = Double.parseDouble(parts[1]);  // Température en Celsius
			double fahrenheit = (celsius * 9 / 5) + 32;  // Conversion de Celsius en Fahrenheit
			String humidity = parts[2];  // Humidité
			return station + "," + fahrenheit + "," + humidity;  // Retour des données avec la température convertie
		});

		// Regroupement des données par station météorologique
		KGroupedStream<String, String> groupedStream = convertedStream.groupBy(
				(key, value) -> value.split(",")[0],  // Clé de regroupement : le nom de la station
				Grouped.with(Serdes.String(), Serdes.String())  // Sérialisation des clés et valeurs en String
		);

		// Calcul des moyennes de température et d'humidité par station
		KTable<String, String> aggregatedTable = groupedStream.aggregate(
				() -> "0.0,0.0,0",  // Valeur initiale (température totale, humidité totale, nombre de mesures)
				(station, newValue, aggregate) -> {
					try {
						// Séparation des nouvelles données et des données agrégées existantes
						String[] newValueParts = newValue.split(",");
						String[] aggregateParts = aggregate.split(",");

						// Vérification de la validité des données
						if (newValueParts.length < 3 || aggregateParts.length < 3) {
							throw new IllegalArgumentException("Invalid data format");
						}

						// Calcul des nouvelles moyennes
						double newTemp = Double.parseDouble(newValueParts[1]);
						double newHumidity = Double.parseDouble(newValueParts[2]);
						double totalTemp = Double.parseDouble(aggregateParts[0]) + newTemp;
						double totalHumidity = Double.parseDouble(aggregateParts[1]) + newHumidity;
						int count = Integer.parseInt(aggregateParts[2]) + 1;  // Incrémentation du nombre de mesures

						// Retour des nouvelles valeurs agrégées
						return totalTemp + "," + totalHumidity + "," + count;
					} catch (Exception e) {
						System.err.println("Error aggregating record: " + newValue + " - " + e.getMessage());
						return aggregate;  // En cas d'erreur, retourner l'agrégat actuel sans modification
					}
				},
				Materialized.with(Serdes.String(), Serdes.String())  // Sérialisation des clés et valeurs de l'agrégation
		);

		// Calcul des moyennes de température et d'humidité et formatage des résultats pour le topic de sortie
		KStream<String, String> resultStream = aggregatedTable.toStream().mapValues(aggregate -> {
			String[] parts = aggregate.split(",");
			double totalTemp = Double.parseDouble(parts[0]);  // Température totale
			double totalHumidity = Double.parseDouble(parts[1]);  // Humidité totale
			int count = Integer.parseInt(parts[2]);  // Nombre de mesures

			// Calcul des moyennes
			double avgTemp = totalTemp / count;
			double avgHumidity = totalHumidity / count;

			// Retour du résultat formaté
			return "Température Moyenne = " + avgTemp + "F, Humidité Moyenne = " + avgHumidity + "%";
		});

		// Envoi des résultats dans un topic Kafka "station-averages"
		resultStream.to("station-averages", Produced.with(Serdes.String(), Serdes.String()));  // Envoi des résultats dans un topic avec sérialisation des clés et valeurs en String

		// Création et démarrage de l'application Kafka Streams
		KafkaStreams streams = new KafkaStreams(builder.build(), props);
		streams.start();  // Démarrage de l'application Kafka Streams

		// Ajout d'un hook pour arrêter proprement l'application lors de la fermeture
		Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
	}
}
