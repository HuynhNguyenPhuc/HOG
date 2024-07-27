package classifier;
import java.io.IOException;

import libsvm.*;

public class SVMHelper {

    public static svm_model fit(float[][] training_data, float[] labels, double C) {
        svm_problem problem = new svm_problem();
        int dataCount = training_data.length;
        problem.y = new double[dataCount];
        problem.x = new svm_node[dataCount][];
        problem.l = dataCount;

        for (int i = 0; i < dataCount; i++) {
            float[] features = training_data[i];
            problem.x[i] = new svm_node[features.length];
            for (int j = 0; j < features.length; j++) {
                svm_node node = new svm_node();
                node.index = j + 1;
                node.value = features[j];
                problem.x[i][j] = node;
            }
            problem.y[i] = labels[i];
        }

        svm_parameter param = getLinearParam(C);

        return svm.svm_train(problem, param);
    }

    public static svm_model fit(float[][] training_data, float[] labels, double C, double gamma) {
        svm_problem problem = new svm_problem();
        int dataCount = training_data.length;
        problem.y = new double[dataCount];
        problem.x = new svm_node[dataCount][];
        problem.l = dataCount;

        for (int i = 0; i < dataCount; i++) {
            float[] features = training_data[i];
            problem.x[i] = new svm_node[features.length];
            for (int j = 0; j < features.length; j++) {
                svm_node node = new svm_node();
                node.index = j + 1;
                node.value = features[j];
                problem.x[i][j] = node;
            }
            problem.y[i] = labels[i];
        }

        svm_parameter param = getRBFParam(gamma, C);

        return svm.svm_train(problem, param);
    }

    public static void save(svm_model model, String filePath) {
        try {
            svm.svm_save_model(filePath, model);
            System.out.println("Model saved successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to save the model.");
        }
    }

    public static svm_model load(String filePath) {
        try {
            svm_model model = svm.svm_load_model(filePath);
            System.out.println("Model loaded successfully.");
            return model;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load the model.");
            return null;
        }
    }

    public static double predict(svm_model model, float[] features)  {
        svm_node[] nodes = new svm_node[features.length];
        for (int i = 0; i < features.length; i++) {
            svm_node node = new svm_node();
            node.index = i + 1;
            node.value = features[i];
            nodes[i] = node;
        }

        return svm.svm_predict(model, nodes);
    }

    public static double[] predict_probability(svm_model model, float[] features) {
        double[] prob_estimates = new double[2];
        svm_node[] nodes = new svm_node[features.length];
        for (int i = 0; i < features.length; i++) {
            svm_node node = new svm_node();
            node.index = i + 1;
            node.value = features[i];
            nodes[i] = node;
        }
        svm.svm_predict_probability(model, nodes, prob_estimates);
        return prob_estimates;
    }

    public static double crossValidation(float[][] training_data, float[] labels, double C, double gamma, int nr_fold, String mode) {
        int dataCount = training_data.length;
        double[] target = new double[dataCount];

        svm_problem problem = new svm_problem();
        problem.y = new double[dataCount];
        problem.x = new svm_node[dataCount][];
        problem.l = dataCount;

        for (int i = 0; i < dataCount; i++) {
            float[] features = training_data[i];
            problem.x[i] = new svm_node[features.length];
            for (int j = 0; j < features.length; j++) {
                svm_node node = new svm_node();
                node.index = j + 1;
                node.value = features[j];
                problem.x[i][j] = node;
            }
            problem.y[i] = labels[i];
        }

        svm_parameter param;

        if (mode == "linear") param = getLinearParam(C);
        else if (mode == "RBF") param = getRBFParam(gamma, C);
        else throw new IllegalArgumentException("Unknown mode!");

        svm.svm_cross_validation(problem, param, nr_fold, target);

        int correct = 0;
        for (int i = 0; i < dataCount; i++) {
            if (target[i] == labels[i]) {
                correct++;
            }
        }
        return 100.0 * correct / dataCount;
    }

    public static svm_model gridSearch(float[][] training_data, float[] labels, double[] C_values, double[] gamma_values, int nr_fold, String mode) {
        double bestC = 0;
        double bestGamma = 0;
        double bestAccuracy = 0;

        for (double C : C_values) {
            for (double gamma : gamma_values) {
                double accuracy = crossValidation(training_data, labels, C, gamma, nr_fold, mode);
                System.out.println("C: " + C + ", gamma: " + gamma + ", Accuracy: " + accuracy);

                if (accuracy > bestAccuracy) {
                    bestAccuracy = accuracy;
                    bestC = C;
                    bestGamma = gamma;
                }
            }
        }

        System.out.println("Best C: " + bestC + ", Best Gamma: " + bestGamma + ", Best Accuracy: " + bestAccuracy);
        return fit(training_data, labels, bestC);
    }

    public static svm_parameter getLinearParam(double C){
        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.LINEAR;
        param.degree = 3;
        param.gamma = 0;
        param.coef0 = 0;
        param.nu = 0.5;
        param.cache_size = 100;
        param.C = C;
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 1;
        param.probability = 1;
        param.nr_weight = 0;
        param.weight_label = new int[0];
        param.weight = new double[0];

        return param;
    }

    public static svm_parameter getRBFParam(double gamma, double C){
        svm_parameter param = new svm_parameter();
        param.probability = 1;
        param.gamma = gamma;
        param.nu = 0.3;
        param.C = C;
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;  // Changed to RBF kernel for grid search
        param.cache_size = 2000;
        param.eps = 0.001;
        return param;
    }
}