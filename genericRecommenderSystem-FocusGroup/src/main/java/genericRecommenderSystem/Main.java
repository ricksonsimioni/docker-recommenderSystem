package genericRecommenderSystem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolModule;

import com.google.common.collect.BiMap;
import net.librec.conf.Configuration;
import net.librec.data.model.TextDataModel;
import net.librec.recommender.Recommender;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.cf.ItemKNNRecommender;
import net.librec.recommender.item.RecommendedList;
import net.librec.similarity.CosineSimilarity;
import net.librec.similarity.RecommenderSimilarity;
import net.librec.eval.RecommenderEvaluator;
import net.librec.eval.ranking.*;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.math.structure.SymmMatrix;
import net.librec.recommender.item.KeyValue;
import net.librec.eval.EvalContext;
import net.librec.data.DataModel;

public class Main {

    static {
        java.util.Map<String, Object> extensionMap = Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap();
        extensionMap.put("ecore", new EcoreResourceFactoryImpl());
        extensionMap.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
    }

    static final String LIBREC_INPUT_FILENAME = "librec_input.txt";
    private static RecommendedList lastRecommendedList;
    private static TextDataModel lastDataModel;
    private static double lastNdcg, lastPrecision, lastRecall, lastF1;

    public static void main(String[] args) {
        try {
            Properties props = loadConfigFromArgs();
            
            // This method now correctly extracts the ratings list from the map
            List<Map<String, Object>> ratingsList = generateRatings(props);
            
            if (ratingsList == null || ratingsList.isEmpty()) {
                System.out.println("No rating data available from the EOL script. Exiting.");
                return;
            }
            
            Path tmpDir = Paths.get(props.getProperty("output.tmp")).toAbsolutePath();
            Files.createDirectories(tmpDir);
            Path librecDataFile = tmpDir.resolve(LIBREC_INPUT_FILENAME);
            writeLibrecInputFile(librecDataFile.toFile(), ratingsList);
            
            Configuration conf = prepareLibRecConfiguration(tmpDir, props);
            TextDataModel dataModel = new TextDataModel(conf);
            dataModel.buildDataModel();

            RecommenderContext context = new RecommenderContext(conf, dataModel);
            RecommenderSimilarity similarity = new CosineSimilarity();
            similarity.buildSimilarityMatrix(dataModel);
            context.setSimilarity(similarity);
            
            Recommender recommender = new ItemKNNRecommender();
            recommender.setContext(context);
            recommender.train(context);
            lastRecommendedList = recommender.recommendRank();
            lastDataModel = dataModel;
            
            // FIXED: Pass both the recommendations and the dataModel to the method
            dumpRecommendations(lastRecommendedList, lastDataModel);
            
            evaluateRecommendations(conf, recommender, dataModel, context, lastRecommendedList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Map<String, Object>> generateRatings(Properties props) throws Exception {
        registerMetamodels(props);

        IEolModule module = new EolModule();
        module.parse(new File(props.getProperty("eol.script")));
        module.getContext().setOutputStream(System.out);

        String[] modelKeys = props.getProperty("models.to.load", "").split(",");
        for (String key : modelKeys) {
            key = key.trim();
            if (key.isEmpty()) continue;
            
            String name = props.getProperty("model." + key + ".name");
            String path = props.getProperty("model." + key + ".path");
            String nsUri = props.getProperty("model." + key + ".metamodel_uri");
            
            EmfModel model = loadEmfModel(name, path, nsUri);
            module.getContext().getModelRepository().addModel(model);
            System.out.println("Loaded model '" + name + "' from: " + path);
        }

        Object result = module.execute();
        module.getContext().getModelRepository().dispose();

        // --- UPDATED LOGIC TO HANDLE THE MAP RETURNED BY EOL ---
        if (result instanceof Map) {
            Map<?, ?> extractedData = (Map<?, ?>) result;
            Object ratingsObj = extractedData.get("ratingsData");
            
            if (ratingsObj instanceof List) {
                System.out.println("Successfully extracted 'ratingsData' list from EOL script's result map.");
                return (List<Map<String, Object>>) ratingsObj;
            }
        }
        
        // Return empty list if the structure is not what's expected
        return new ArrayList<>();
    }
    
    public static void registerMetamodels(Properties props) throws IOException {
        ResourceSet rs = new ResourceSetImpl();
        String[] metamodelKeys = props.getProperty("metamodels.to.register", "").split(",");
        for (String key : metamodelKeys) {
            key = key.trim();
            if (key.isEmpty()) continue;
            String path = props.getProperty("metamodel." + key + ".path");
            EPackage ePackage = (EPackage) rs.getResource(URI.createFileURI(path), true).getContents().get(0);
            EPackage.Registry.INSTANCE.put(ePackage.getNsURI(), ePackage);
            System.out.println("Registered metamodel '" + key + "' from: " + path);
        }
    }

    public static EmfModel loadEmfModel(String name, String modelPath, String metamodelUri) throws Exception {
        EmfModel model = new EmfModel();
        model.setName(name);
        model.setModelFile(modelPath);
        model.setMetamodelUri(metamodelUri);
        model.load();
        return model;
    }

    private static Properties loadConfigFromArgs() throws IOException {
        Properties props = new Properties();
        String configFile = System.getProperty("config.file");
        if (configFile == null || !new File(configFile).exists()) {
            throw new FileNotFoundException("Provide config file via -Dconfig.file=<path>");
        }
        try (InputStream in = new FileInputStream(configFile)) {
            props.load(in);
        }
        return props;
    }
    
    public static void writeLibrecInputFile(File outFile, List<Map<String, Object>> ratingsList) throws IOException {
        Map<String, Integer> userMap = new HashMap<>();
        Map<String, Integer> itemMap = new HashMap<>();
        int uCounter = 0, iCounter = 0;
        for (Map<String, Object> r : ratingsList) {
            Object uStrObj = r.get("userId");
            Object iStrObj = r.get("itemId");
            if (uStrObj == null || iStrObj == null) continue;
            
            String uStr = uStrObj.toString();
            String iStr = iStrObj.toString();
            if (!userMap.containsKey(uStr)) userMap.put(uStr, uCounter++);
            if (!itemMap.containsKey(iStr)) itemMap.put(iStr, iCounter++);
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
            for (Map<String, Object> r : ratingsList) {
                Object uStrObj = r.get("userId");
                Object iStrObj = r.get("itemId");
                if (uStrObj == null || iStrObj == null) continue;

                String uStr = uStrObj.toString();
                String iStr = iStrObj.toString();
                Object v = r.get("rating");
                int userOut = userMap.get(uStr) + 1;
                int itemOut = itemMap.get(iStr) + 1;
                bw.write(userOut + "\t" + itemOut + "\t" + v + "\n");
            }
        }
    }
    
    static Configuration prepareLibRecConfiguration(Path tmpDir, Properties props) {
        Configuration conf = new Configuration();
        conf.set("dfs.data.dir", tmpDir.toString());
        conf.set("data.input.path", LIBREC_INPUT_FILENAME);
        conf.set("rec.recommender.similarity.key", props.getProperty("rec.similarity.key", "item"));
        conf.setBoolean("rec.recommender.isranking", Boolean.parseBoolean(props.getProperty("rec.isranking", "true")));
        conf.setInt("rec.similarity.shrinkage", Integer.parseInt(props.getProperty("rec.similarity.shrinkage", "10")));
        conf.set("rec.neighbors.knn.number", props.getProperty("rec.knn", "200"));
        return conf;
    }
    
    static void dumpRecommendations(RecommendedList recommendedList, TextDataModel dataModel) {
        if (recommendedList == null || dataModel == null) {
            System.out.println("Recommendation list or data model is null. Cannot dump results.");
            return;
        }
        System.out.println("\n=== Recommendations Result ===");
        
        // Get the mappings from LibRec's internal integers back to your original String IDs
        BiMap<String, Integer> userMapping = dataModel.getUserMappingData();
        BiMap<String, Integer> itemMapping = dataModel.getItemMappingData();

        // Iterate through each user in the result list
        for (int userIdx = 0; userIdx < recommendedList.size(); userIdx++) {
            // Get the original user ID (e.g., "viewer1")
            String userId = userMapping.inverse().get(userIdx);
            List<KeyValue<Integer, Double>> recsForUser = recommendedList.getKeyValueListByContext(userIdx);

            if (recsForUser != null && !recsForUser.isEmpty()) {
                System.out.println("\nRecommendations for User '" + userId + "':");
                for (KeyValue<Integer, Double> kv : recsForUser) {
                    // Get the original item ID (e.g., "tt0468569")
                    String itemId = itemMapping.inverse().get(kv.getKey());
                    System.out.printf("  - Item: %-15s | Score: %.4f%n", itemId, kv.getValue());
                }
            }
        }
        System.out.println("============================");
    }
    
    private static void evaluateRecommendations(Configuration conf, Recommender recommender, DataModel dataModel,
            RecommenderContext context, RecommendedList recommendedList) throws Exception {
        
        // FIXED: Replaced the incorrect .isEmpty() call with the correct .size() == 0 check.
        if (recommendedList == null || dataModel.getTestDataSet() == null || dataModel.getTestDataSet().size() == 0) {
            System.out.println("Evaluation skipped: No recommendations or test data available.");
            lastNdcg = lastPrecision = lastRecall = lastF1 = 0.0;
            return;
        }
        
        EvalContext evalContext = new EvalContext(
                conf,
                recommender,
                dataModel.getTestDataSet()
        );
        
        RecommenderEvaluator ndcgEvaluator = new NormalizedDCGEvaluator();
        ndcgEvaluator.setTopN(10);
        lastNdcg = ndcgEvaluator.evaluate(evalContext);
        
        RecommenderEvaluator precisionEval = new PrecisionEvaluator();
        precisionEval.setTopN(10);
        lastPrecision = precisionEval.evaluate(evalContext);

        RecommenderEvaluator recallEval = new RecallEvaluator();
        recallEval.setTopN(10);
        lastRecall = recallEval.evaluate(evalContext);
        
        lastF1 = (lastPrecision + lastRecall) > 0 ? 2 * lastPrecision * lastRecall / (lastPrecision + lastRecall) : 0.0;
        
        System.out.printf("Evaluation Metrics -> NDCG: %.4f, Precision: %.4f, Recall: %.4f, F1: %.4f%n", lastNdcg, lastPrecision, lastRecall, lastF1);
    }
    
    public static RecommendedList getLastRecommendedList() { return lastRecommendedList; }
    public static TextDataModel getLastDataModel() { return lastDataModel; }
    public static double getLastNdcg() { return lastNdcg; }
    public static double getLastPrecision() { return lastPrecision; }
    public static double getLastRecall() { return lastRecall; }
    public static double getLastF1() { return lastF1; }
}