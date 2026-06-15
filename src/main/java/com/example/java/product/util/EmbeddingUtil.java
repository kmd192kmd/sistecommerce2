package com.example.java.product.util;

import java.util.Random;

public class EmbeddingUtil {

    public static float[] getEmbedding(String text) {
        float[] vector = new float[128];
        if (text == null || text.isEmpty()) {
            return vector;
        }
        
        long seed = text.hashCode();
        Random random = new Random(seed);
        float sum = 0.0f;
        for (int i = 0; i < 128; i++) {
            vector[i] = (float) random.nextGaussian();
            sum += vector[i] * vector[i];
        }
        
        float norm = (float) Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < 128; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }
}
