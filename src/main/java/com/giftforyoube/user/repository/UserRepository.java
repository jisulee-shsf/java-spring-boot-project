package com.giftforyoube.user.repository;

import com.giftforyoube.donation.entity.Donation;
import com.giftforyoube.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
