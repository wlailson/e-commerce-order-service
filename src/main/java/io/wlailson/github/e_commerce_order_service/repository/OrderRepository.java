package io.wlailson.github.e_commerce_order_service.repository;

import io.wlailson.github.e_commerce_order_service.domain.Order;
import io.wlailson.github.e_commerce_order_service.projections.OrderMinResponseProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByClientIdAndId(Long clientId, Long orderId);

    @Query(value = """
            SELECT obj.id AS id, obj.moment AS moment, obj.status AS status, SUM(i.price * i.quantity) AS total
            FROM Order obj
            JOIN obj.items i
            WHERE obj.clientId = :clientId
            AND obj.status = UPPER(:status)
            GROUP BY obj.id, obj.moment, obj.status
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT obj)
                    FROM Order obj
                    JOIN obj.items i
                    WHERE obj.clientId = :clientId
                    AND obj.status = UPPER(:status)
                    """)
    Page<OrderMinResponseProjection> searchAllOrdersByClientIdAndStatus(
            Pageable pageable,
            @Param("clientId") Long clientId,
            @Param("status") String status);
}
