package financial;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.aeonbits.owner.ConfigCache;

import autofe.db.configuration.DatabaseAutoFeConfiguration;
import autofe.processor.DatabaseProcessor;
import de.upb.crc901.mlplan.multiclass.DefaultPreorder;
import de.upb.crc901.mlplan.multiclass.MLPlan;
import jaicore.basic.SQLAdapter;
import jaicore.experiments.ExperimentDBEntry;
import jaicore.experiments.ExperimentRunner;
import jaicore.experiments.IExperimentIntermediateResultProcessor;
import jaicore.experiments.IExperimentSetConfig;
import jaicore.experiments.IExperimentSetEvaluator;
import jaicore.ml.WekaUtil;
import weka.classifiers.Evaluation;
import weka.core.Instances;

public class FinancialExperimentRunner {

	public static void main(String[] args) {
		FinancialConfig m = ConfigCache.getOrCreate(FinancialConfig.class);

		ExperimentRunner runner = new ExperimentRunner(new IExperimentSetEvaluator() {

			@Override
			public IExperimentSetConfig getConfig() {
				return m;
			}

			@Override
			public void evaluate(ExperimentDBEntry experimentEntry, SQLAdapter adapter,
					IExperimentIntermediateResultProcessor processor) throws Exception {
				// Get experiment setup
				Map<String, String> description = experimentEntry.getExperiment().getValuesOfKeyFields();
				int timeoutInMs = Integer.parseInt(description.get("timeout"));
				long seed = Long.parseLong(description.get("seed"));
				int randomCompletionPathLength = Integer.parseInt(description.get("randomcompletion_pathlength"));
				String evaluationFunction = description.get("evaluation_function");
				String dbModelFile = m.getDatabaseModelFile();
				DatabaseAutoFeConfiguration config = new DatabaseAutoFeConfiguration(randomCompletionPathLength,
						evaluationFunction, seed, timeoutInMs);
				DatabaseProcessor dbProcessor = new DatabaseProcessor(config, dbModelFile);

				// Get features
				Instances instances = dbProcessor.selectFeatures();

				// Evaluate features using ML-Plan
				List<Instances> split = WekaUtil.getStratifiedSplit(instances, new Random(0), .7f);
				MLPlan mlplan = new MLPlan(new File("model/weka/weka-all-autoweka.json"));
				mlplan.setLoggerName("mlplan");
				mlplan.setTimeout(timeoutInMs);
				mlplan.setPortionOfDataForPhase2(.3f);
				mlplan.setNodeEvaluator(new DefaultPreorder());
				mlplan.enableVisualization();
				mlplan.buildClassifier(split.get(0));

				Evaluation eval = new Evaluation(split.get(0));
				eval.evaluateModel(mlplan, split.get(1));
				double accuracy = eval.pctCorrect();

				Map<String, Object> results = new HashMap<>();
				results.put("score", accuracy);
				processor.processResults(results);
			}
		});

		runner.randomlyConductExperiments(true);

	}

}
