package com.yuxiang.commuity.repository;

import com.yuxiang.commuity.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.test.database.replace=ANY",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=never"
})
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveProduct_canReadFromDatabase() {
        Product product = createProduct("测试苹果", "6.20", 10);

        // 保存并执行数据库写入
        Product saved = productRepository.saveAndFlush(product);
        Long id = saved.getId();

        assertThat(id).isNotNull();

        // 清除缓存，让后面的查询重新读取数据库
        entityManager.clear();

        Product found = productRepository.findById(id).orElseThrow();

        assertThat(found.getName()).isEqualTo("测试苹果");
        assertThat(found.getPrice())
                .isEqualByComparingTo(new BigDecimal("6.20"));
        assertThat(found.getStock()).isEqualTo(10);
    }

    @Test
    void searchByName_returnsOnlyMatchingProducts() {
        productRepository.saveAll(List.of(
                createProduct("红苹果", "6.20", 10),
                createProduct("青苹果", "7.50", 20),
                createProduct("香蕉", "3.50", 30)
        ));
        productRepository.flush();
        entityManager.clear();

        List<Product> results =
                productRepository.findByNameContainingIgnoreCase("苹果");

        assertThat(results)
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("红苹果", "青苹果");
    }

    private Product createProduct(String name, String price, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Repository 自动化测试");
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setStatus("ON_SALE");
        return product;
    }
}