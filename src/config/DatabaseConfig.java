package config;

import jaicore.experiments.IExperimentSetConfig;

public abstract interface DatabaseConfig extends IExperimentSetConfig {

	public static final String TIMEOUT = "timeout";
	public static final String SEED = "seed";
	public static final String RC_PATHLENGTH = "randomcompletion_pathlength";
	public static final String EVALUATION_FUNCTION = "evaluation_function";
	public static final String DATABASE_MODEL_FILE = "database_modelfile";
	public static final String FE_FRACTION = "fe_fraction";

	@Key(TIMEOUT)
	public int getTimeout();

	@Key(SEED)
	public long getSeed();

	@Key(RC_PATHLENGTH)
	public int getRCPathLength();

	@Key(EVALUATION_FUNCTION)
	public String getEvaluationFunction();

	@Key(DATABASE_MODEL_FILE)
	public String getDatabaseModelFile();

	@Key(FE_FRACTION)
	public double getFeFraction();

}
