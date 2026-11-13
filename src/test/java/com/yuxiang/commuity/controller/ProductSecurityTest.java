package com.yuxiang.commuity.controller;

import com.yuxiang.commuity.Product;
import com.yuxiang.commuity.config.SecurityConfig;
import com.yuxiang.commuity.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRepository productRepository;

    @Test
    void anonymousUser_canSearchProducts() throws Exception {
        given(productRepository.findByNameContainingIgnoreCase("苹果"))
                .willReturn(List.of());

        mockMvc.perform(get("/api/products/search")
                        .param("name", "苹果"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(productRepository).findByNameContainingIgnoreCase("苹果");
    }

    @Test
    void anonymousUser_canCreateProduct_withoutCsrfToken() throws Exception {
        given(productRepository.save(any(Product.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "安全配置测试商品",
                                  "description": "验证匿名新增",
                                  "price": 9.90,
                                  "stock": 5,
                                  "status": "ON_SALE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("安全配置测试商品"));

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void anonymousUser_canDeleteProduct_withoutCsrfToken() throws Exception {
        given(productRepository.existsById(1L)).willReturn(true);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isOk());

        verify(productRepository).deleteById(1L);
    }

    @Test
    void anonymousUser_cannotAccessProtectedPath() throws Exception {
        mockMvc.perform(get("/private/security-check"))
                .andExpect(status().isForbidden());
    }
}