package financial;

import org.aeonbits.owner.Config.Sources;

import jaicore.experiments.IExperimentSetConfig;

@Sources({ "file:conf/financial/financial.properties" })
public interface FinancialConfig extends IExperimentSetConfig{
	
	public static final String TIMEOUT = "timeout";
	public static final String SEED = "seed";
	public static final String RC_PATHLENGTH = "randomcompletion_pathlength";
	public static final String EVALUATION_FUNCTION = "evaluation_function";
	public static final String DATABASE_MODEL_FILE = "database_modelfile";

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


}
