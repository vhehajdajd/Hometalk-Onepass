package com.hometalk.onepass.community.service;

import com.hometalk.onepass.community.dto.request.BoardRequestDTO;
import com.hometalk.onepass.community.dto.response.BoardResponseDTO;
import com.hometalk.onepass.community.entity.Board;
import com.hometalk.onepass.community.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;

    public List<BoardResponseDTO> findAll() {
        return boardRepository.findAll().stream()
            .map(board -> BoardResponseDTO.from(board))
            .collect(Collectors.toList());
    }

    public BoardResponseDTO findById(Long id) {
        return boardRepository.findById(id).map(board -> BoardResponseDTO.from(board)).orElse(null);
    }

    public BoardResponseDTO findByCode(String code) {
        return boardRepository.findByCode(code).map(BoardResponseDTO::from).orElse(null);
    }

    public BoardResponseDTO findByName(String name) {
        return boardRepository.findByName(name).map(BoardResponseDTO::from).orElse(null);
    }

    // 게시판 생성
    @Transactional
    public BoardResponseDTO save(BoardRequestDTO boardRequestDTO) {
        Board board = Board.builder()
                .name(boardRequestDTO.getName())
                .code(boardRequestDTO.getCode())
                .build();
        Board saved = boardRepository.save(board);
        return BoardResponseDTO.from(saved);
    }
}
