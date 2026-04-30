package com.hometalk.onepass.community.config;

import com.hometalk.onepass.community.entity.Board;
import com.hometalk.onepass.community.entity.Category;
import com.hometalk.onepass.community.repository.BoardRepository;
import com.hometalk.onepass.community.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ComInitDataConfig implements CommandLineRunner {

    private final BoardRepository boardRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        // 1. Board 초기 데이터 삽입
        List<Board> boards = boardRepository.findAll();

        if (boards.isEmpty()) {
            Board square = boardRepository.save(Board.builder().name("광장").code("square").build());
            Board market = boardRepository.save(Board.builder().name("마켓").code("market").build());
            Board talk = boardRepository.save(Board.builder().name("소통").code("talk").build());

            // 2. 생성된 게시판 객체(square, market)를 사용하여 Category 연결
            // 광장 카테고리
            categoryRepository.save(Category.builder().name("전체").code("all").board(square).build());
            categoryRepository.save(Category.builder().name("자유").code("free").board(square).build());
            categoryRepository.save(Category.builder().name("토론").code("debate").board(square).build());

            categoryRepository.save(Category.builder().name("나눔").code("share").board(market).build());
            categoryRepository.save(Category.builder().name("분실물").code("lost").board(market).build());

            categoryRepository.save(Category.builder().name("설문").code("survey").board(talk).build());

            log.info("초기 데이터 생성 완료");
        } else {
            log.info("기존 데이터가 존재합니다.");
        }
    }
}
