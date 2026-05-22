package com.trekmate.backend.repository;
import com.trekmate.backend.model.Wishlist;
import com.trekmate.backend.model.embeddable.WishlistId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, WishlistId> {
    List<Wishlist> findByUserId(UUID userId);
}
