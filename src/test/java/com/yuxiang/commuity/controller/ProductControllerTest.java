package com.yuxiang.commuity.controller;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.mockito.Mockito.verify;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import java.util.Optional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import static org.mockito.ArgumentMatchers.any;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.yuxiang.commuity.Product;
import com.yuxiang.commuity.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRepository productRepository;

    @Test
    void searchProducts_returnsMatchingProducts() throws Exception {
        Product product = new Product();
        product.setName("测试苹果");
        product.setPrice(new BigDecimal("6.20"));

        given(productRepository.findByNameContainingIgnoreCase("测试"))
                .willReturn(List.of(product));

        mockMvc.perform(get("/api/products/search")
                        .param("name", "测试"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("测试苹果"));
    }

    @Test
    void getProductsPage_withValidParameters_returns200() throws Exception {
        given(productRepository.findAll(any(Pageable.class)))
                .willReturn(Page.<Product>empty(PageRequest.of(0, 10)));

        mockMvc.perform(get("/api/products/page")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.pageable").doesNotExist());
    }

    @Test
    void getProductsPage_withNegativePage_returns400() throws Exception {
        mockMvc.perform(get("/api/products/page")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.page").exists());
    }

    @Test
    void getProduct_whenNotFound_returns404() throws Exception {
        given(productRepository.findById(999L))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProduct_withValidData_returns200() throws Exception {
        Product savedProduct = new Product();
        savedProduct.setName("自动化测试商品");
        savedProduct.setDescription("测试新增接口");
        savedProduct.setPrice(new BigDecimal("9.90"));
        savedProduct.setStock(5);
        savedProduct.setStatus("ON_SALE");

        given(productRepository.save(any(Product.class)))
                .willReturn(savedProduct);

        mockMvc.perform(post("/api/products")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "自动化测试商品",
                              "description": "测试新增接口",
                              "price": 9.90,
                              "stock": 5,
                              "status": "ON_SALE"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("自动化测试商品"));
    }
    @Test
    void createProduct_withBlankName_returns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": " ",
                              "description": "测试空名称",
                              "price": 9.90,
                              "stock": 5,
                              "status": "ON_SALE"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    void createProduct_withNegativePrice_returns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "价格校验测试",
                              "description": "测试负价格",
                              "price": -1,
                              "stock": 5,
                              "status": "ON_SALE"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.price").exists());
    }
    @Test
    void updateProduct_withValidData_returns200() throws Exception {
        // 模拟数据库里已有的商品
        Product existingProduct = new Product();
        existingProduct.setName("原商品");
        existingProduct.setDescription("原描述");
        existingProduct.setPrice(new BigDecimal("5.00"));
        existingProduct.setStock(10);
        existingProduct.setStatus("ON_SALE");

        given(productRepository.findById(1L))
                .willReturn(Optional.of(existingProduct));

        // 模拟保存：返回实际传给 save 的商品
        given(productRepository.save(any(Product.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "修改后的商品",
                              "description": "修改后的描述",
                              "price": 12.50,
                              "stock": 20,
                              "status": "ON_SALE"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("修改后的商品"))
                .andExpect(jsonPath("$.description").value("修改后的描述"))
                .andExpect(jsonPath("$.price").value(12.50))
                .andExpect(jsonPath("$.stock").value(20));

        // 确认修改后的商品确实交给了 Repository 保存
        verify(productRepository).save(existingProduct);
    }
    @Test
    void updateProduct_whenNotFound_returns404() throws Exception {
        given(productRepository.findById(999L))
                .willReturn(Optional.empty());

        mockMvc.perform(put("/api/products/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "修改商品测试",
                              "description": "测试不存在的商品",
                              "price": 9.90,
                              "stock": 5,
                              "status": "ON_SALE"
                            }
                            """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProduct_whenExists_returns200() throws Exception {
        // 模拟商品存在
        given(productRepository.existsById(1L)).willReturn(true);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isOk());

        // 确认调用了删除方法，且商品 ID 正确
        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteProduct_whenNotFound_returns404() throws Exception {
        // 模拟商品不存在
        given(productRepository.existsById(999L)).willReturn(false);

        mockMvc.perform(delete("/api/products/999"))
                .andExpect(status().isNotFound());

        // 商品不存在时，不应执行删除
        verify(productRepository, never()).deleteById(any(Long.class));
    }
}