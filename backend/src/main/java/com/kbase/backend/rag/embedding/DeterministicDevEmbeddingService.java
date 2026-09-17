package com.kbase.backend.rag.embedding;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Deterministic hashing embeddings for development and tests only; not semantic production AI. */
@Service
@ConditionalOnProperty(name = "app.embedding.provider", havingValue = "dev", matchIfMissing = true)
public class DeterministicDevEmbeddingService implements EmbeddingService {

    private final EmbeddingProperties properties;

    public DeterministicDevEmbeddingService(EmbeddingProperties properties) {
        if (properties.dimension() <= 0) {
            throw new IllegalArgumentException("Embedding dimension must be positive");
        }
        this.properties = properties;
    }

    @Override
    public EmbeddingResult embed(String text) {
        double[] vector = new double[properties.dimension()];
        for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (token.isBlank()) {
                continue;
            }
            byte[] hash = sha256(token);
            int index = Math.floorMod(java.nio.ByteBuffer.wrap(hash, 0, 4).getInt(), vector.length);
            double sign = (hash[4] & 1) == 0 ? 1.0 : -1.0;
            vector[index] += sign;
        }
        double norm = Math.sqrt(java.util.Arrays.stream(vector).map(value -> value * value).sum());
        List<Double> values = new ArrayList<>(vector.length);
        for (double value : vector) {
            values.add(norm == 0 ? 0.0 : value / norm);
        }
        return new EmbeddingResult(values, properties.model(), vector.length);
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
