package com.gitops.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitops.app.dto.ProductDTO;
import com.gitops.app.exception.ResourceNotFoundException;
import com.gitops.app.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductDTO buildSampleDTO() {
        ProductDTO dto = new ProductDTO();
        dto.setId(1L);
        dto.setName("Laptop");
        dto.setPrice(new BigDecimal("999.99"));
        dto.setStockQuantity(20);
        dto.setCategory("Electronics");
        return dto;
    }

    @Test
    void getAllProducts_returns200WithList() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(buildSampleDTO()));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[0].category").value("Electronics"));
    }

    @Test
    void getProduct_existingId_returns200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(buildSampleDTO());

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getProduct_nonExistingId_returns404() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProduct_validBody_returns201() throws Exception {
        ProductDTO dto = buildSampleDTO();
        dto.setId(null);
        when(productService.createProduct(any())).thenReturn(buildSampleDTO());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createProduct_missingName_returns400() throws Exception {
        ProductDTO dto = new ProductDTO();
        dto.setPrice(new BigDecimal("10.00"));
        dto.setCategory("Electronics");
        dto.setStockQuantity(5);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void deleteProduct_existingId_returns204() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isNoContent());
    }
}
