package com.data.personalfinanceinsightai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.data.personalfinanceinsightai.config.GeminiProperties;
import com.data.personalfinanceinsightai.dto.response.transaction.CategorizationResult;
import com.data.personalfinanceinsightai.dto.response.transaction.ResultSource;
import com.data.personalfinanceinsightai.entity.Category;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.repository.CategoryRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategorizationServiceTest {

    @Mock
    private GeminiClient geminiClient;
    @Mock
    private CategorizationCacheService cacheService;
    @Mock
    private AiCallLogService logService;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AiRateLimiter rateLimiter;
    @Mock
    private GeminiProperties geminiProperties;

    @InjectMocks
    private CategorizationService service;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");
    }

    // ── Cache hit ──────────────────────────────────────────────

    @Test
    @DisplayName("Cache hit → trả kết quả từ cache, không gọi Gemini")
    void categorize_cacheHit_noGeminiCall() {
        when(cacheService.isTooShort(any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        Category diLai = mockCategory(5L, "Đi lại");
        when(cacheService.getCachedCategory("Grab đi làm"))
                .thenReturn(Optional.of(diLai));

        CategorizationResult result = service.categorize("Grab đi làm", 1L);

        assertTrue(result.isSuccessful());
        assertEquals(ResultSource.CACHE, result.getSource());
        assertEquals(5L, result.getCategoryId());
        // Gemini phải không được gọi
        verify(geminiClient, never()).chat(any(), any());
        // Cache không lưu lại (đã có)
        verify(cacheService, never()).cacheResult(any(), any());
    }

    // ── Cache miss → Gemini thành công ────────────────────────

    @Test
    @DisplayName("Cache miss → Gemini trả đúng category → lưu cache + log success")
    void categorize_cacheMiss_geminiSuccess() {
        Category diLai = mockCategory(5L, "Đi lại");

        when(cacheService.isTooShort(any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(cacheService.getCachedCategory(any())).thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(1L)).thenReturn(true);
        when(geminiClient.chat(any(), any())).thenReturn("Đi lại");
        when(categoryRepository.findByNameIgnoreCase("Đi lại"))
                .thenReturn(Optional.of(diLai));
        when(geminiProperties.getModel()).thenReturn("gemini-2.5-flash");

        CategorizationResult result = service.categorize("Grab đi làm", 1L);

        assertTrue(result.isSuccessful());
        assertEquals(ResultSource.AI, result.getSource());
        assertEquals(5L, result.getCategoryId());
        assertEquals("Đi lại", result.getSuggestedCategoryName());
        // Phải lưu cache
        verify(cacheService).cacheResult(eq("Grab đi làm"), eq(diLai));
        // Phải log success
        verify(logService).logSuccess(eq(mockUser), eq("CATEGORIZATION"),
                eq("gemini-2.5-flash"), anyInt(), anyInt(), anyInt());
    }

    // ── Gemini trả null/empty ──────────────────────────────────

    @Test
    @DisplayName("Gemini trả null → FAILED, không crash, log failure")
    void categorize_geminiReturnsNull_failed() {
        when(cacheService.isTooShort(any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(cacheService.getCachedCategory(any())).thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(any())).thenReturn(true);
        when(geminiClient.chat(any(), any())).thenReturn(null);

        CategorizationResult result = service.categorize("Grab đi làm", 1L);

        assertFalse(result.isSuccessful());
        assertEquals(ResultSource.FAILED, result.getSource());
        assertNull(result.getCategoryId());
        assertNotNull(result.getMessage());
        // Không được lưu cache
        verify(cacheService, never()).cacheResult(any(), any());
        // Log failure
        verify(logService).logFailure(eq(mockUser), eq("CATEGORIZATION"), anyString());
    }

    @Test
    @DisplayName("Gemini trả empty string → FAILED")
    void categorize_geminiReturnsEmpty_failed() {
        when(cacheService.isTooShort(any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(cacheService.getCachedCategory(any())).thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(any())).thenReturn(true);
        when(geminiClient.chat(any(), any())).thenReturn("   ");

        CategorizationResult result = service.categorize("Grab đi làm", 1L);

        assertFalse(result.isSuccessful());
        assertEquals(ResultSource.FAILED, result.getSource());
    }

    // ── Unknown category → fallback ───────────────────────────

    @Test
    @DisplayName("Gemini trả category không có trong DB → fallback sang 'Khác'")
    void categorize_unknownCategory_fallbackToKhac() {
        Category khac = mockCategory(15L, "Khác");

        when(cacheService.isTooShort(any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(cacheService.getCachedCategory(any())).thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(any())).thenReturn(true);
        when(geminiClient.chat(any(), any())).thenReturn("Danh mục lạ XYZ");
        when(categoryRepository.findByNameIgnoreCase("Danh mục lạ XYZ"))
                .thenReturn(Optional.empty());
        when(categoryRepository.findByName("Khác"))
                .thenReturn(Optional.of(khac));
        when(geminiProperties.getModel()).thenReturn("gemini-2.5-flash");

        CategorizationResult result = service.categorize("abc xyz", 1L);

        assertTrue(result.isSuccessful());
        assertEquals(15L, result.getCategoryId());
        assertEquals("Khác", result.getSuggestedCategoryName());
    }

    @Test
    @DisplayName("Gemini trả unknown + fallback 'Khác' cũng missing → FAILED")
    void categorize_unknownCategory_fallbackAlsoMissing_failed() {
        when(cacheService.isTooShort(any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(cacheService.getCachedCategory(any())).thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(any())).thenReturn(true);
        when(geminiClient.chat(any(), any())).thenReturn("Danh mục lạ");
        when(categoryRepository.findByNameIgnoreCase("Danh mục lạ"))
                .thenReturn(Optional.empty());
        when(categoryRepository.findByName("Khác"))
                .thenReturn(Optional.empty());

        CategorizationResult result = service.categorize("abc", 1L);

        assertFalse(result.isSuccessful());
        assertEquals(ResultSource.FAILED, result.getSource());
    }

    // ── Rate limit ────────────────────────────────────────────

    @Test
    @DisplayName("Rate limit vượt → FAILED, không gọi Gemini")
    void categorize_rateLimitExceeded_failed() {
        when(cacheService.isTooShort(any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(cacheService.getCachedCategory(any())).thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(1L)).thenReturn(false);

        CategorizationResult result = service.categorize("Grab đi làm", 1L);

        assertFalse(result.isSuccessful());
        assertEquals(ResultSource.FAILED, result.getSource());
        assertNotNull(result.getMessage());
        // Gemini phải KHÔNG được gọi
        verify(geminiClient, never()).chat(any(), any());
    }

    // ── Description quá ngắn ──────────────────────────────────

    @Test
    @DisplayName("Description quá ngắn (< 2 ký tự) → SKIPPED, không gọi gì cả")
    void categorize_tooShort_skipped() {
        when(cacheService.isTooShort("a")).thenReturn(true);

        CategorizationResult result = service.categorize("a", 1L);

        assertEquals(ResultSource.SKIPPED, result.getSource());
        assertFalse(result.isSuccessful());
        // Không gọi Gemini, không check cache, không log
        verify(geminiClient, never()).chat(any(), any());
        verify(cacheService, never()).getCachedCategory(any());
        verify(logService, never()).logSuccess(any(), any(), any(), anyInt(), anyInt(), anyInt());
    }

    // ── Gemini throws exception → fallback ────────────────────

    @Test
    @DisplayName("Gemini throws RuntimeException → FAILED, transaction vẫn OK")
    void categorize_geminiThrowsException_failed() {
        when(cacheService.isTooShort(any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(cacheService.getCachedCategory(any())).thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(any())).thenReturn(true);
        when(geminiClient.chat(any(), any()))
                .thenThrow(new RuntimeException("Connection timeout"));

        CategorizationResult result = service.categorize("Grab đi làm", 1L);

        assertFalse(result.isSuccessful());
        assertEquals(ResultSource.FAILED, result.getSource());
        assertNotNull(result.getMessage());
        // Log failure
        verify(logService).logFailure(eq(mockUser), eq("CATEGORIZATION"), anyString());
    }

    // ── userId null (anonymous) ───────────────────────────────

    @Test
    @DisplayName("userId null → vẫn categorize được, không log")
    void categorize_nullUserId_stillWorks() {
        Category anUong = mockCategory(1L, "Ăn uống");

        when(cacheService.isTooShort(any())).thenReturn(false);
        when(cacheService.getCachedCategory(any())).thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(null)).thenReturn(true);
        when(geminiClient.chat(any(), any())).thenReturn("Ăn uống");
        when(categoryRepository.findByNameIgnoreCase("Ăn uống"))
                .thenReturn(Optional.of(anUong));

        CategorizationResult result = service.categorize("KFC bữa tối", null);

        assertTrue(result.isSuccessful());
        assertEquals(1L, result.getCategoryId());
        // Không log vì user null
        verify(logService, never()).logSuccess(any(), any(), any(), anyInt(), anyInt(), anyInt());
    }

    // ── Helper ────────────────────────────────────────────────

    private Category mockCategory(Long id, String name) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        return c;
    }
}
