package config;

import org.aeonbits.owner.Config.Sources;

@Sources({ "file:conf/experiments/medical.properties" })
public interface MedicalConfig extends DatabaseConfig {

}
