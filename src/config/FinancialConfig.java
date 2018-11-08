package config;

import org.aeonbits.owner.Config.Sources;

@Sources({ "file:conf/experiments/financial.properties" })
public interface FinancialConfig extends DatabaseConfig {

}
