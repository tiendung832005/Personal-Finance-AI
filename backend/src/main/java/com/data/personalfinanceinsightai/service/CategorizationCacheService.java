package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.entity.CategorizationCache;
import com.data.personalfinanceinsightai.entity.Category;
import com.data.personalfinanceinsightai.repository.CategorizationCacheRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategorizationCacheService {

    private final CategorizationCacheRepository cacheRepository;
    private static final int MIN_DESC_LENGTH = 2;

    /** Check cache — trả về category nếu có */
    public Optional<Category> getCachedCategory(String description) {
        if (isTooShort(description)) {
            return Optional.empty();
        }
        
        String hash = hashDescription(description);
        return cacheRepository.findByDescriptionHash(hash)
                .map(cache -> {
                    cache.setHitCount(cache.getHitCount() + 1);
                    cacheRepository.save(cache);
                    log.debug("Cache HIT: hash={}", hash);
                    return cache.getCategory();
                });
    }

    /** Lưu kết quả vào cache (tạo mới hoặc overwrite) */
    public void cacheResult(String description, Category category) {
        if (isTooShort(description) || category == null) {
            return;
        }

        String hash = hashDescription(description);
        CategorizationCache cache = cacheRepository
                .findByDescriptionHash(hash)
                .orElse(new CategorizationCache());
                
        cache.setDescriptionHash(hash);
        cache.setCategory(category);
        cache.setHitCount(cache.getId() != null ? cache.getHitCount() + 1 : 1);
        
        cacheRepository.save(cache);
        log.debug("Cached categoryId={} for hash={}", category.getId(), hash);
    }

    /** Normalize + SHA-256 hash */
    private String hashDescription(String description) {
        String normalized = description.toLowerCase().trim()
                .replaceAll("\\s+", " ");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isTooShort(String description) {
        return description == null || description.trim().length() < MIN_DESC_LENGTH;
    }
}
