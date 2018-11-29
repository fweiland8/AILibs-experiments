package runner;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.aeonbits.owner.ConfigCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import autofe.db.configuration.DatabaseAutoFeConfiguration;
import autofe.db.model.database.AbstractFeature;
import autofe.processor.DatabaseProcessor;
import config.ConfigFactory;
import config.DatabaseConfig;
import de.upb.crc901.mlplan.multiclass.wekamlplan.MLPlanWekaClassifier;
import de.upb.crc901.mlplan.multiclass.wekamlplan.weka.WekaMLPlanWekaClassifier;
import jaicore.basic.SQLAdapter;
import jaicore.experiments.ExperimentDBEntry;
import jaicore.experiments.ExperimentRunner;
import jaicore.experiments.IExperimentIntermediateResultProcessor;
import jaicore.experiments.IExperimentSetConfig;
import jaicore.experiments.IExperimentSetEvaluator;
import jaicore.ml.WekaUtil;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

public class DatabaseExperimentRunner {

	private static Logger LOG = LoggerFactory.getLogger("EXPERIMENT");

	public static void main(String[] args) {
		String dataSetName = args[0];
		if (dataSetName == null || dataSetName.isEmpty()) {
			LOG.error("No data set name provided => Shutting down..");
			System.exit(1);
		}

		Class<? extends DatabaseConfig> configClass = ConfigFactory.getExperimentConfigClass(dataSetName);
		DatabaseConfig config = ConfigCache.getOrCreate(configClass);

		ExperimentRunner runner = new ExperimentRunner(new IExperimentSetEvaluator() {

			@Override
			public IExperimentSetConfig getConfig() {
				return config;
			}

			@Override
			public void evaluate(ExperimentDBEntry experimentEntry, SQLAdapter adapter,
					IExperimentIntermediateResultProcessor processor) throws Exception {
				try {
					doExperiment(experimentEntry, adapter, processor, config);
				} catch (Exception e) {
					LOG.error("Experiment failed!", e);
					throw e;
				}
			}
		});

		runner.randomlyConductExperiments(true);

	}

	private static void doExperiment(ExperimentDBEntry experimentEntry, SQLAdapter adapter,
			IExperimentIntermediateResultProcessor processor, DatabaseConfig config) throws Exception {
		LOG.info("Starting experiment");

		// Get experiment setup
		Map<String, String> description = experimentEntry.getExperiment().getValuesOfKeyFields();
		int timeoutInMs = Integer.parseInt(description.get("timeout"));
		long seed = Long.parseLong(description.get("seed"));
		double feFraction = Double.parseDouble(description.get("fe_fraction"));
		int randomCompletionPathLength = Integer.parseInt(description.get("randomcompletion_pathlength"));
		String evaluationFunction = description.get("evaluation_function");

		LOG.info("Setup is: timeout={}, seed={}, feFraction={}, rcPathLength={}, evalFunction={}", timeoutInMs, seed,
				feFraction, randomCompletionPathLength, evaluationFunction);

		String dbModelFile = config.getDatabaseModelFile();
		DatabaseAutoFeConfiguration dbcConfig = new DatabaseAutoFeConfiguration(randomCompletionPathLength,
				evaluationFunction, seed, (int) (timeoutInMs * feFraction));
		LOG.info("Starting feature extraction..");
		DatabaseProcessor dbProcessor = new DatabaseProcessor(dbcConfig, dbModelFile);
		dbProcessor.doFeatureSelection();
		LOG.info("Finshed feature extraction..");

		// Get features
		List<AbstractFeature> selectedFeatures = dbProcessor.getSelectedFeatures();
		Instances instances = dbProcessor.getInstancesWithSelectedFeatures();

		LOG.info("Selected {} features", selectedFeatures.size());

		// Save instances to ARFF file for debugging purposes
		ArffSaver s = new ArffSaver();
		s.setInstances(instances);
		s.setFile(new File("data.arff"));
		s.writeBatch();

		LOG.info("Starting ML-Plan");

		// Evaluate features using ML-Plan
		List<Instances> split = WekaUtil.getStratifiedSplit(instances, new Random(0), .7f);
		MLPlanWekaClassifier mlplan = new WekaMLPlanWekaClassifier();
		mlplan.setLoggerName("mlplan");
		mlplan.setTimeout((int) ((timeoutInMs / 1000) * (1 - feFraction)));
		mlplan.setTimeoutForNodeEvaluation(15);
		mlplan.setTimeoutForSingleSolutionEvaluation(15);
		mlplan.setPortionOfDataForPhase2(.3f);
		mlplan.buildClassifier(split.get(0));
		mlplan.setNumCPUs(config.getNumberOfCPUs());

		Evaluation eval = new Evaluation(split.get(0));
		eval.evaluateModel(mlplan, split.get(1));
		double accuracy = eval.pctCorrect();

		LOG.info("Finished experiment, score is {}", accuracy);

		Map<String, Object> results = new HashMap<>();
		results.put("score", accuracy);
		results.put("no_selected_features", selectedFeatures.size());
		results.put("selected_features", selectedFeatures.toString());
		processor.processResults(results);
	}

}
