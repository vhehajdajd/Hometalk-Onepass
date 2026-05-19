package com.hometalk.onepass.community.repository;

import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.entity.PostFile;
import com.hometalk.onepass.community.enums.PostFileType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostFileRepository extends JpaRepository<PostFile, Long> {
}
