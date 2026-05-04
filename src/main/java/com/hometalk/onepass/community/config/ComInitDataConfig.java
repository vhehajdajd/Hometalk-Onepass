package com.hometalk.onepass.community.config;

import com.hometalk.onepass.community.entity.Board;
import com.hometalk.onepass.community.entity.Category;
import com.hometalk.onepass.community.entity.Tag;
import com.hometalk.onepass.community.repository.BoardRepository;
import com.hometalk.onepass.community.repository.CategoryRepository;
import com.hometalk.onepass.community.repository.TagRepository;
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
    private final TagRepository tagRepository;

    @Override
    public void run(String... args) throws Exception {
        // 1. Board 초기 데이터 삽입
        List<Board> boards = boardRepository.findAll();

        if (boards.isEmpty()) {
            Board square = boardRepository.save(Board.builder().name("광장").code("square").system(true).build());
            Board market = boardRepository.save(Board.builder().name("마켓").code("market").system(true).build());
            Board talk = boardRepository.save(Board.builder().name("소통").code("talk").system(true).build());

            // 2. 생성된 게시판 객체(square, market)를 사용하여 Category 연결
            // 광장 카테고리
            // 시스템 카테고리는 CSS 클래스(post-list.css)로 디자인을 제어하므로 별도의 bgColor, textColor 설정 X (null 유지)
            categoryRepository.save(Category.builder().name("자유").code("free").board(square).system(true).build());
            categoryRepository.save(Category.builder().name("토론").code("debate").board(square).system(true).build());

            categoryRepository.save(Category.builder().name("나눔").code("share").board(market).system(true).build());
            categoryRepository.save(Category.builder().name("분실물").code("lost").board(market).system(true).build());
            categoryRepository.save(Category.builder().name("거래").code("trade").board(market).system(true).build());

            categoryRepository.save(Category.builder().name("설문").code("survey").board(talk).system(true).build());

            if (tagRepository.count() == 0) {
                List<String> tagNames = List.of("공지", "맛집", "질문", "정보", "이벤트", "꿀팁", "운동");
                tagNames.forEach(name -> tagRepository.save(Tag.builder().name(name).build()));
            }

            log.info("초기 데이터 생성 완료");
        } else {
            log.info("기존 데이터가 존재합니다.");
        }
    }
}
