package com.cronos.order.service;

import com.cronos.order.entity.Order;
import com.cronos.order.entity.OrderStatus;
import com.cronos.order.entity.OrderStatusHistory;
import com.cronos.order.repository.OrderRepository;
import com.cronos.order.repository.OrderStatusHistoryRepository;
import com.cronos.order.security.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

/** Every status change goes through here so it is validated, versioned and recorded in the history. */
@Service
public class OrderStateService {

    public record Change(Order order, boolean changed) {}

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;

    public OrderStateService(OrderRepository orderRepository, OrderStatusHistoryRepository historyRepository) {
        this.orderRepository = orderRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public Order create(Order order) {
        Order saved = orderRepository.saveAndFlush(order);
        historyRepository.save(new OrderStatusHistory(saved.getId(), null, saved.getStatus(), "customer", "Order placed"));
        return saved;
    }

    @Transactional
    public Change transition(UUID orderId, OrderStatus target, String actor, String note, Consumer<Order> extra) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        OrderStatus current = order.getStatus();
        if (current == target) {
            return new Change(order, false);
        }
        if (!current.allowedNext().contains(target)) {
            throw ApiException.conflict("INVALID_TRANSITION", "An order in status " + current + " cannot move to " + target);
        }
        order.setStatus(target);
        if (target == OrderStatus.CANCELLED || target == OrderStatus.REFUNDED) {
            order.setStockReleasePending(true);
        }
        if (extra != null) {
            extra.accept(order);
        }
        historyRepository.save(new OrderStatusHistory(orderId, current, target, actor, note));
        return new Change(orderRepository.saveAndFlush(order), true);
    }

    @Transactional
    public void markStockReleased(UUID orderId) {
        orderRepository.findById(orderId).ifPresent(o -> o.setStockReleasePending(false));
    }

    @Transactional
    public void annotate(UUID orderId, String note) {
        orderRepository.findById(orderId).ifPresent(o ->
                o.setNotes(o.getNotes() == null ? note : o.getNotes() + "\n" + note));
    }
}
