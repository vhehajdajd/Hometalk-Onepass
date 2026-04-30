package com.hometalk.onepass.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "household")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Household {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "building_name", nullable = false, length = 100)
    private String buildingName;

    @NotNull
    @Column(name = "dong", nullable = false, length = 20)
    private String dong;

    @NotNull
    @Column(name = "ho", nullable = false, length = 20)
    private String ho;

    @NotNull
    @Column(name = "post_num", nullable = false)
    private String postNum;

    @OneToMany(mappedBy = "household", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    @Builder
    public Household(String buildingName, String dong, String ho, String postNum) {
        this.buildingName = buildingName;
        this.dong = dong;
        this.ho = ho;
        this.postNum = postNum;
    }
}
