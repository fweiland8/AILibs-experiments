package config;

public class ConfigFactory {

	public static Class<? extends DatabaseConfig> getExperimentConfigClass(String dataSetName) {
		switch (dataSetName.toUpperCase()) {
		case "FINANCIAL":
			return FinancialConfig.class;
		case "MEDICAL":
			return MedicalConfig.class;
		default:
			throw new RuntimeException("Invalid data set name : " + dataSetName);
		}
	}

}
