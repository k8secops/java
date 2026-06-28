package com.gitops.app.service;

import com.gitops.app.dto.OrderDTO;
import com.gitops.app.dto.OrderItemDTO;
import com.gitops.app.exception.InsufficientStockException;
import com.gitops.app.exception.ResourceNotFoundException;
import com.gitops.app.model.*;
import com.gitops.app.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;

    public OrderService(OrderRepository orderRepository, ProductService productService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        return toDTO(findById(id));
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByCustomerEmail(String email) {
        return orderRepository.findByCustomerEmail(email).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public OrderDTO createOrder(OrderDTO dto) {
        Order order = new Order(dto.getCustomerName(), dto.getCustomerEmail());

        for (OrderItemDTO itemDTO : dto.getItems()) {
            Product product = productService.findById(itemDTO.getProductId());
            if (product.getStockQuantity() < itemDTO.getQuantity()) {
                throw new InsufficientStockException(
                        product.getName(), itemDTO.getQuantity(), product.getStockQuantity());
            }
            product.setStockQuantity(product.getStockQuantity() - itemDTO.getQuantity());
            order.addItem(new OrderItem(product, itemDTO.getQuantity()));
        }

        return toDTO(orderRepository.save(order));
    }

    public OrderDTO updateOrderStatus(Long id, OrderStatus status) {
        Order order = findById(id);
        order.setStatus(status);
        return toDTO(orderRepository.save(order));
    }

    public void cancelOrder(Long id) {
        Order order = findById(id);
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel a delivered order");
        }
        // Return stock
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private OrderDTO toDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setCustomerName(order.getCustomerName());
        dto.setCustomerEmail(order.getCustomerEmail());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setItems(order.getItems().stream().map(this::toItemDTO).collect(Collectors.toList()));
        return dto;
    }

    private OrderItemDTO toItemDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        return dto;
    }
}
