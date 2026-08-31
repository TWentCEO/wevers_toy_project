package com.weverse.ticketing.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "membership_code", length = 50)
    private String membershipCode;

    @Column(name = "fan_club_name", length = 100)
    private String fanClubName;

    @Builder
    public UserProfile(User user, String phoneNumber, String membershipCode, String fanClubName) {
        this.user = user;
        this.phoneNumber = phoneNumber;
        this.membershipCode = membershipCode;
        this.fanClubName = fanClubName;
    }
}
